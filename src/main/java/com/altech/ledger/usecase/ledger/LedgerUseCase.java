package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.ledger.LedgerDto.*;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.entity.po.JournalEntry;
import com.altech.ledger.entity.po.JournalTransaction;
import com.altech.ledger.entity.po.LedgerAccount;
import com.altech.ledger.repository.JournalEntryRepository;
import com.altech.ledger.repository.JournalTransactionRepository;
import com.altech.ledger.repository.LedgerAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LedgerUseCase {
    private final LedgerAccountRepository accounts;
    private final JournalTransactionRepository transactions;
    private final JournalEntryRepository entries;

    public LedgerUseCase(LedgerAccountRepository accounts, JournalTransactionRepository transactions,
                         JournalEntryRepository entries) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.entries = entries;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        requireLedgerCurrency(request.currency());
        if (accounts.existsByExternalReference(request.externalReference())) {
            throw LedgerException.conflict("EXTERNAL_REFERENCE_EXISTS", "External reference already exists");
        }
        return accountResponse(accounts.save(new LedgerAccount(request.externalReference(), request.name(),
            request.type(), request.currency(), request.allowNegative())));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        return accountResponse(account(id));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID id) {
        LedgerAccount account = account(id);
        BigDecimal debits = zero(entries.debitTotal(id));
        BigDecimal credits = zero(entries.creditTotal(id));
        return new BalanceResponse(id, account.getCurrency(), debits, credits, signed(account, debits, credits));
    }

    @Transactional(readOnly = true)
    public PageResponse<EntryResponse> getEntries(UUID id, Pageable pageable) {
        account(id);
        Page<JournalEntry> page = entries.findByAccountId(id, pageable);
        return new PageResponse<>(page.map(this::entryResponse).getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public PostingResult post(PostTransactionRequest request) {
        List<NormalizedEntry> normalized = normalize(request.entries());
        String hash = postHash(request, normalized);
        Optional<JournalTransaction> existing = transactions.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) {
                throw LedgerException.conflict("IDEMPOTENCY_CONFLICT",
                    "Idempotency key was already used with a different payload");
            }
            return new PostingResult(transactionResponse(existing.get()), false);
        }

        Map<UUID, LedgerAccount> locked = lockAccounts(normalized.stream()
            .map(NormalizedEntry::accountId).collect(Collectors.toSet()));
        validateEntries(normalized, locked);
        validateNonnegative(normalized, locked);

        JournalTransaction tx = new JournalTransaction(request.idempotencyKey(), hash, request.reference(),
            request.description(), request.effectiveAt() == null ? Instant.now() : request.effectiveAt(), null);
        for (NormalizedEntry item : normalized) {
            JournalEntry entry = new JournalEntry(tx, locked.get(item.accountId()), item.side(), item.amount(),
                item.currency(), item.sequence());
            tx.addEntry(entry);
        }
        return new PostingResult(transactionResponse(transactions.save(tx)), true);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id) {
        return transactionResponse(transactions.findWithEntriesById(id)
            .orElseThrow(() -> LedgerException.notFound("Transaction not found: " + id)));
    }

    @Transactional
    public PostingResult reverse(UUID originalId, ReversalRequest request) {
        String hash = sha256("REVERSAL|" + originalId + "|" + value(request.description()));
        Optional<JournalTransaction> byKey = transactions.findByIdempotencyKey(request.idempotencyKey());
        if (byKey.isPresent()) {
            JournalTransaction existing = byKey.get();
            if (!existing.getRequestHash().equals(hash)
                || existing.getReversalOf() == null
                || !existing.getReversalOf().getId().equals(originalId)) {
                throw LedgerException.conflict("IDEMPOTENCY_CONFLICT",
                    "Idempotency key was already used with a different payload");
            }
            return new PostingResult(transactionResponse(existing), false);
        }

        JournalTransaction original = transactions.findWithEntriesById(originalId)
            .orElseThrow(() -> LedgerException.notFound("Transaction not found: " + originalId));
        Set<UUID> accountIds = original.getEntries().stream()
            .map(e -> e.getAccount().getId()).collect(Collectors.toSet());
        Map<UUID, LedgerAccount> locked = lockAccounts(accountIds);
        Optional<JournalTransaction> priorReversal = transactions.findByReversalOfId(originalId);
        if (priorReversal.isPresent()) {
            throw LedgerException.conflict("ALREADY_REVERSED", "Transaction has already been reversed");
        }

        List<NormalizedEntry> reversed = original.getEntries().stream()
            .map(e -> new NormalizedEntry(e.getAccount().getId(), opposite(e.getSide()), e.getAmount(),
                e.getCurrency(), e.getSequence()))
            .toList();
        validateNonnegative(reversed, locked);

        JournalTransaction reversal = new JournalTransaction(request.idempotencyKey(), hash,
            "REVERSAL:" + value(original.getReference()), request.description(), Instant.now(), original);
        for (NormalizedEntry item : reversed) {
            JournalEntry entry = new JournalEntry(reversal, locked.get(item.accountId()), item.side(),
                item.amount(), item.currency(), item.sequence());
            reversal.addEntry(entry);
        }
        original.markReversed();
        return new PostingResult(transactionResponse(transactions.save(reversal)), true);
    }

    private List<NormalizedEntry> normalize(List<EntryRequest> requested) {
        boolean anySequence = requested.stream().anyMatch(e -> e.sequence() != null);
        boolean allSequence = requested.stream().allMatch(e -> e.sequence() != null);
        if (anySequence && !allSequence) {
            throw LedgerException.badRequest("INVALID_SEQUENCE", "Provide all sequence numbers or none");
        }
        Set<Integer> seen = new HashSet<>();
        List<NormalizedEntry> result = new ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            EntryRequest e = requested.get(i);
            int sequence = allSequence ? e.sequence() : i + 1;
            if (!seen.add(sequence)) {
                throw LedgerException.badRequest("DUPLICATE_SEQUENCE", "Entry sequence numbers must be unique");
            }
            result.add(new NormalizedEntry(e.accountId(), e.side(), e.amount(), e.currency(), sequence));
        }
        return result.stream().sorted(Comparator.comparingInt(NormalizedEntry::sequence)).toList();
    }

    private Map<UUID, LedgerAccount> lockAccounts(Set<UUID> ids) {
        List<UUID> sorted = ids.stream().sorted().toList();
        Map<UUID, LedgerAccount> locked = accounts.lockAllById(sorted).stream()
            .collect(Collectors.toMap(LedgerAccount::getId, Function.identity()));
        if (locked.size() != ids.size()) {
            Set<UUID> missing = new LinkedHashSet<>(sorted);
            missing.removeAll(locked.keySet());
            throw LedgerException.notFound("Accounts not found: " + missing);
        }
        return locked;
    }

    private void validateEntries(List<NormalizedEntry> normalized, Map<UUID, LedgerAccount> locked) {
        Map<String, BigDecimal[]> totals = new HashMap<>();
        for (NormalizedEntry item : normalized) {
            LedgerAccount account = locked.get(item.accountId());
            if (account.getStatus() != LedgerAccount.Status.ACTIVE) {
                throw LedgerException.conflict("ACCOUNT_NOT_ACTIVE", "Account is not active: " + account.getId());
            }
            if (!account.getCurrency().equals(item.currency())) {
                throw LedgerException.badRequest("CURRENCY_MISMATCH",
                    "Entry currency does not match account " + account.getId());
            }
            if (item.amount() == null || item.amount().signum() <= 0) {
                throw LedgerException.badRequest("INVALID_AMOUNT", "Entry amounts must be positive");
            }
            BigDecimal[] sides = totals.computeIfAbsent(item.currency(),
                ignored -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            int side = item.side() == JournalEntry.Side.DEBIT ? 0 : 1;
            sides[side] = sides[side].add(item.amount());
        }
        totals.forEach((currency, sides) -> {
            if (sides[0].compareTo(sides[1]) != 0) {
                throw LedgerException.badRequest("UNBALANCED_TRANSACTION",
                    "Debits and credits do not balance for " + currency);
            }
        });
    }

    private void validateNonnegative(List<NormalizedEntry> normalized, Map<UUID, LedgerAccount> locked) {
        Map<UUID, BigDecimal> deltas = new HashMap<>();
        for (NormalizedEntry item : normalized) {
            LedgerAccount account = locked.get(item.accountId());
            BigDecimal delta = normalIncrease(account.getType(), item.side())
                ? item.amount() : item.amount().negate();
            deltas.merge(account.getId(), delta, BigDecimal::add);
        }
        for (Map.Entry<UUID, BigDecimal> change : deltas.entrySet()) {
            LedgerAccount account = locked.get(change.getKey());
            if (!account.isAllowNegative()) {
                BigDecimal current = signed(account, zero(entries.debitTotal(account.getId())),
                    zero(entries.creditTotal(account.getId())));
                if (current.add(change.getValue()).signum() < 0) {
                    throw LedgerException.conflict("INSUFFICIENT_BALANCE",
                        "Posting would make account balance negative: " + account.getId());
                }
            }
        }
    }

    private String postHash(PostTransactionRequest request, List<NormalizedEntry> normalized) {
        StringBuilder canonical = new StringBuilder("POST|")
            .append(value(request.reference())).append('|')
            .append(value(request.description())).append('|')
            .append(request.effectiveAt() == null ? "" : request.effectiveAt()).append('|');
        normalized.forEach(e -> canonical.append(e.sequence()).append(':').append(e.accountId()).append(':')
            .append(e.side()).append(':').append(decimal(e.amount())).append(':').append(e.currency()).append(';'));
        return sha256(canonical.toString());
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private LedgerAccount account(UUID id) {
        return accounts.findById(id).orElseThrow(() -> LedgerException.notFound("Account not found: " + id));
    }

    private BigDecimal signed(LedgerAccount account, BigDecimal debits, BigDecimal credits) {
        return switch (account.getType()) {
            case ASSET, EXPENSE -> debits.subtract(credits);
            case LIABILITY, EQUITY, REVENUE -> credits.subtract(debits);
        };
    }

    private boolean normalIncrease(LedgerAccount.Type type, JournalEntry.Side side) {
        boolean debitNormal = type == LedgerAccount.Type.ASSET || type == LedgerAccount.Type.EXPENSE;
        return debitNormal ? side == JournalEntry.Side.DEBIT : side == JournalEntry.Side.CREDIT;
    }

    private JournalEntry.Side opposite(JournalEntry.Side side) {
        return side == JournalEntry.Side.DEBIT ? JournalEntry.Side.CREDIT : JournalEntry.Side.DEBIT;
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String decimal(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
    private String value(String value) { return value == null ? "" : value; }

    private void requireLedgerCurrency(String currency) {
        if (currency == null || !currency.matches("[A-Z]{2,4}")) {
            throw LedgerException.badRequest("INVALID_CURRENCY", "Currency must be 2-4 uppercase letters");
        }
    }

    private AccountResponse accountResponse(LedgerAccount a) {
        return new AccountResponse(a.getId(), a.getExternalReference(), a.getName(), a.getType(), a.getCurrency(),
            a.getStatus(), a.isAllowNegative(), a.getVersion(), a.getCreatedAt(), a.getUpdatedAt());
    }

    private EntryResponse entryResponse(JournalEntry e) {
        return new EntryResponse(e.getId(), e.getTransaction().getId(), e.getAccount().getId(), e.getSide(),
            e.getAmount(), e.getCurrency(), e.getSequence(), e.getCreatedAt());
    }

    private TransactionResponse transactionResponse(JournalTransaction tx) {
        return new TransactionResponse(tx.getId(), tx.getIdempotencyKey(), tx.getReference(), tx.getDescription(),
            tx.getStatus(), tx.getEffectiveAt(), tx.getCreatedAt(),
            tx.getReversalOf() == null ? null : tx.getReversalOf().getId(),
            tx.getEntries().stream().map(this::entryResponse).toList());
    }

    private record NormalizedEntry(UUID accountId, JournalEntry.Side side, BigDecimal amount,
                                   String currency, int sequence) {}
    public record PostingResult(TransactionResponse transaction, boolean created) {}
}
