package com.altech.ledger.usecase.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.exception.response.AccountErrorResponse;

import com.altech.ledger.entity.dto.BalanceExecutionResultCommand;
import com.altech.ledger.entity.dto.event.BalanceUpdatedEvent;
import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.enu.BalanceOperation;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerEntry;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.listener.intf.LedgerHandler;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerEntryRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.MovementBus;
import com.altech.ledger.entity.po.accounting.AccountingRule;
import com.altech.ledger.entity.po.accounting.AccountingRuleExecution;
import com.altech.ledger.usecase.coa.CoaBookResolver;
import com.altech.ledger.usecase.ingest.ProgramPoolService;
import com.altech.ledger.usecase.rule.AccountingRuleCatalogUseCase;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * LedgerMovementExecutionUseCase.
 * Structure: fetch rules → rulesExecution (command) → updateBalances → ledger ledgerEntryRepository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerMovementExecutionUseCase implements LedgerHandler {
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    /** @Lazy breaks cycle MovementBus ↔ this use case */
    @Lazy
    private final MovementBus movementBus;
    private final AccountingRuleCatalogUseCase accountingRuleCatalogUseCase;
    private final CoaBookResolver coaBookResolver;
    private final ProgramPoolService programPoolService;

    @Override
    @Transactional
    public void execute(LedgerMovementEvent event) {
        if (event.getMovementId() == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "movementId required on event");
        }
        LedgerMovement movement = ledgerMovementRepository.findById(event.getMovementId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Movement not found: " + event.getMovementId()));
        execute(movement);
    }

    @Override
    public List<Account> fetchAccounts(String identifier) {
        try {
            Long id = Long.valueOf(identifier);
            return accountRepository.findById(id).map(List::of).orElse(List.of());
        } catch (NumberFormatException ex) {
            return List.of();
        }
    }

    @Override
    @Transactional
    public void updateBalances(LedgerMovementEvent event) {
        execute(event);
    }

    @Override
    public void notification(LedgerMovementEvent event) {
        log.info("movement notification id={} status={}", event.getMovementId(), event.getStatus());
    }

    @Transactional
    public LedgerMovement execute(LedgerMovement movement) {
        log.info("execute movement id={} orderType={} amount={}", movement.getId(),
            movement.getOrderType(), movement.getAmount());
        if (movement.getStatus() == LedgerMovementStatus.SETTLED) {
            return movement;
        }
        try {
            BalanceExecutionResultCommand command = rulesExecution(movement);
            applyCommand(command, movement);
            stampMemberBookCurrency(movement, command);
            movement.setStatus(LedgerMovementStatus.SETTLED);
            ledgerMovementRepository.save(movement);
            createLedgerEntries(movement, command);
            movementBus.publishDone(movement);
            movementBus.publishBalanceUpdated(buildBalanceUpdatedEvent(movement, command));
            notification(MovementBus.toEvent(movement));
        } catch (RuntimeException ex) {
            log.error("movement execution failed id={}", movement.getId(), ex);
            movement.setStatus(LedgerMovementStatus.ERROR);
            ledgerMovementRepository.save(movement);
            throw ex;
        }
        return movement;
    }

    /**
     * Posting sequence from {@link AccountingRuleExecution} when present;
     * otherwise OrderType switch (deposit / withdraw / transfer / hold).
     */
    public BalanceExecutionResultCommand rulesExecution(LedgerMovement movement) {
        BalanceExecutionResultCommand fromRules = executeAccountingRules(movement);
        if (fromRules != null) {
            return fromRules;
        }
        BalanceExecutionResultCommand command = new BalanceExecutionResultCommand();
        BigDecimal amount = movement.getAmount();
        Currency currency = movement.getCurrency();

        switch (movement.getOrderType()) {
            case DEPOSIT, PAYMENT_LINK -> {
                Account target = resolveAccount(movement.getTargetId(), currency);
                command.add(target, amount, BalanceOperation.ADD);
            }
            case WITHDRAWAL -> {
                Account origin = resolveAccount(movement.getOriginatorId(), currency);
                command.add(origin, amount, BalanceOperation.SUBTRACT);
            }
            case WALLET_TRANSFER, IN_WALLET_TRANSFER -> {
                Account origin = resolveAccount(movement.getOriginatorId(), currency);
                Account target = resolveAccount(movement.getTargetId(), currency);
                command.add(origin, amount, BalanceOperation.SUBTRACT);
                command.add(target, amount, BalanceOperation.ADD);
            }
            case SWIFT_TRANSFER -> {
                Account origin = resolveAccount(movement.getOriginatorId(), currency);
                command.add(origin, amount, BalanceOperation.SUBTRACT);
            }
            case EARN, ADJUSTMENT -> {
                // Double-entry fallback: DEBIT HOUSE operating + CREDIT customer (same currency)
                Account customer = resolveAccount(movement.getTargetId() != null
                    ? movement.getTargetId() : String.valueOf(movement.getWalletId()), currency);
                Account pool = programPoolService.ensurePoolAccount(currency);
                command.add(pool, amount, BalanceOperation.SUBTRACT);
                command.add(customer, amount, BalanceOperation.ADD);
            }
            case BURN, BANK_CHARGE, HANDLING_CHARGE, CHARGE -> {
                Account customer = resolveAccount(movement.getOriginatorId() != null
                    ? movement.getOriginatorId() : String.valueOf(movement.getWalletId()), currency);
                if (movement.getOrderType() == OrderType.BURN) {
                    Account pool = programPoolService.ensurePoolAccount(currency);
                    command.add(customer, amount, BalanceOperation.SUBTRACT);
                    command.add(pool, amount, BalanceOperation.ADD);
                } else {
                    command.add(customer, amount, BalanceOperation.SUBTRACT);
                }
            }
            case HOLD -> {
                Account customer = resolveAccount(movement.getOriginatorId() != null
                    ? movement.getOriginatorId() : String.valueOf(movement.getWalletId()), currency);
                command.add(customer, amount, BalanceOperation.HOLD_LOCK);
            }
            case RELEASE -> {
                Account customer = resolveAccount(movement.getTargetId() != null
                    ? movement.getTargetId() : String.valueOf(movement.getWalletId()), currency);
                command.add(customer, amount, BalanceOperation.HOLD_UNLOCK);
            }
            default -> throw new BizException(MovementErrorResponse.MOV0400, "Unsupported order type: " + movement.getOrderType());
        }
        return command;
    }

    /**
     * Walk AccountingRule legs. Member targetAccount (no house walletId) resolves against
     * {@code movement.walletId} so each CUST gets a different account id.
     * {@code movement.mainAccount} selects the member product tree (9089 vs 9088); house ignores it.
     */
    private BalanceExecutionResultCommand executeAccountingRules(LedgerMovement movement) {
        OrderType orderType = movement.getOrderType();
        if (orderType == OrderType.ADJUSTMENT_REFUND) {
            return reverseOriginalLegs(movement);
        }
        if (orderType != OrderType.EARN && orderType != OrderType.BURN && orderType != OrderType.ADJUSTMENT) {
            return null;
        }
        accountingRuleCatalogUseCase.ensureDefault();
        Optional<AccountingRuleExecution> seq = accountingRuleCatalogUseCase.findSequence(
            movement.getEvent(), orderType, movement.getCurrency());
        if (seq.isEmpty()) {
            return null;
        }
        List<AccountingRule> rules = accountingRuleCatalogUseCase.loadRules(seq.get());
        if (rules.isEmpty()) {
            return null;
        }
        log.info("posting sequence {} eventType={} legs={}",
            seq.get().getName(), seq.get().getEventType(), rules.size());
        BalanceExecutionResultCommand command = new BalanceExecutionResultCommand();
        BigDecimal base = movement.getAmount();
        for (AccountingRule rule : rules) {
            Account account = coaBookResolver.resolve(
                rule.getTargetAccount(), movement.getWalletId(), movement.getMainAccount());
            BigDecimal amt = rule.getMultiplier() == null ? base : base.multiply(rule.getMultiplier());
            BalanceOperation op = rule.getDirection() == MovementDirection.CREDIT
                ? BalanceOperation.ADD
                : BalanceOperation.SUBTRACT;
            command.add(account, amt, op);
        }
        return command;
    }

    /**
     * Replay original legs with DR/CR swapped (CREDIT→DEBIT, DEBIT→CREDIT).
     * Amounts stay the original magnitude; the refund movement stores the negated total.
     */
    private BalanceExecutionResultCommand reverseOriginalLegs(LedgerMovement refund) {
        Long origId = refund.getAssociatedLedgerMovementId();
        if (origId == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "refund requires associatedLedgerMovementId");
        }
        List<LedgerEntry> orig = ledgerEntryRepository.findByTxnId(origId);
        if (orig == null || orig.isEmpty()) {
            throw new BizException(MovementErrorResponse.MOV0400, "original movement has no legs to reverse");
        }
        BalanceExecutionResultCommand command = new BalanceExecutionResultCommand();
        for (LedgerEntry e : orig) {
            Long accountId;
            try {
                accountId = Long.valueOf(e.getTargetId());
            } catch (Exception ex) {
                throw new BizException(MovementErrorResponse.MOV0400, "original leg missing account: " + e.getId());
            }
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + accountId));
            BigDecimal amt = e.getAmount() == null ? BigDecimal.ZERO : e.getAmount().abs();
            BalanceOperation op = e.getDirection() == MovementDirection.CREDIT
                ? BalanceOperation.SUBTRACT
                : BalanceOperation.ADD;
            command.add(account, amt, op);
        }
        return command;
    }

    private void applyCommand(BalanceExecutionResultCommand command, LedgerMovement movement) {
        for (BalanceExecutionResultCommand.CommandDetail detail : command.getDetails()) {
            apply(detail, movement);
        }
    }

    private void apply(BalanceExecutionResultCommand.CommandDetail cmd, LedgerMovement movement) {
        Account locked = accountRepository.lockById(cmd.getAccount().getId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + cmd.getAccount().getId()));
        BigDecimal ledger = locked.getLedgerBalance();
        BigDecimal available = locked.getAvailableBalance();
        BalanceOperation op = cmd.getOperation();
        if (op == BalanceOperation.HOLD_LOCK) {
            // ledger unchanged; lock spendable balance
            if (!locked.isAllowNegative() && available.compareTo(cmd.getAmount()) < 0) {
                throw new BizException(MovementErrorResponse.MOV0403,
                    "Insufficient available to hold on account " + locked.getId());
            }
            available = available.subtract(cmd.getAmount());
        } else if (op == BalanceOperation.HOLD_UNLOCK) {
            // ledger unchanged; unlock — available cannot exceed ledger
            BigDecimal next = available.add(cmd.getAmount());
            if (next.compareTo(ledger) > 0) {
                throw new BizException(MovementErrorResponse.MOV0400,
                    "Release would make available > ledger on account " + locked.getId());
            }
            available = next;
        } else if (op == BalanceOperation.ADD) {
            ledger = ledger.add(cmd.getAmount());
            available = available.add(cmd.getAmount());
        } else {
            if (!locked.isAllowNegative() && available.compareTo(cmd.getAmount()) < 0) {
                throw new BizException(MovementErrorResponse.MOV0403, "Insufficient available balance on account " + locked.getId());
            }
            ledger = ledger.subtract(cmd.getAmount());
            available = available.subtract(cmd.getAmount());
        }
        locked.setLedgerBalance(ledger);
        locked.setAvailableBalance(available);
        accountRepository.save(locked);
    }

    /**
     * Wallet history shows this movement's currency. Brain may score LP while accounting
     * books HKD (or the reverse) — stamp the member book's currency so history matches the books.
     */
    private void stampMemberBookCurrency(LedgerMovement movement, BalanceExecutionResultCommand command) {
        if (movement == null || movement.getWalletId() == null || command == null || command.getDetails() == null) {
            return;
        }
        Long walletId = movement.getWalletId();
        Account preferred = null;
        Account any = null;
        for (BalanceExecutionResultCommand.CommandDetail d : command.getDetails()) {
            Account a = d.getAccount();
            if (a == null || a.getCurrency() == null || !walletId.equals(a.getWalletId())) {
                continue;
            }
            any = a;
            boolean credit = d.getOperation() == BalanceOperation.ADD
                || d.getOperation() == BalanceOperation.HOLD_UNLOCK;
            boolean debit = d.getOperation() == BalanceOperation.SUBTRACT
                || d.getOperation() == BalanceOperation.HOLD_LOCK;
            if (movement.getOrderType() == OrderType.EARN && credit) {
                preferred = a;
                break;
            }
            if (movement.getOrderType() == OrderType.BURN && debit) {
                preferred = a;
                break;
            }
        }
        Account book = preferred != null ? preferred : any;
        if (book != null) {
            movement.setCurrency(book.getCurrency());
        }
    }

    private void createLedgerEntries(LedgerMovement movement, BalanceExecutionResultCommand command) {
        for (BalanceExecutionResultCommand.CommandDetail cmd : command.getDetails()) {
            MovementDirection direction = switch (cmd.getOperation()) {
                case ADD, HOLD_UNLOCK -> MovementDirection.CREDIT;
                case SUBTRACT, HOLD_LOCK -> MovementDirection.DEBIT;
            };
            boolean holdLike = cmd.getOperation() == BalanceOperation.HOLD_LOCK
                || cmd.getOperation() == BalanceOperation.HOLD_UNLOCK;
            LedgerEntry entry = new LedgerEntry();
            entry.setTxnId(movement.getId());
            entry.setTargetId(String.valueOf(cmd.getAccount().getId()));
            entry.setAmount(cmd.getAmount());
            entry.setDirection(direction);
            entry.setCurrency(cmd.getAccount() != null && cmd.getAccount().getCurrency() != null
                ? cmd.getAccount().getCurrency()
                : movement.getCurrency());
            entry.setAffectsLedger(!holdLike);
            entry.setAffectsAvailable(true);
            ledgerEntryRepository.save(entry);
        }
    }

    private BalanceUpdatedEvent buildBalanceUpdatedEvent(
        LedgerMovement movement,
        BalanceExecutionResultCommand command
    ) {
        String ownerId = null;
        if (movement.getWalletId() != null) {
            ownerId = walletRepository.findById(movement.getWalletId())
                .map(Wallet::getOwnerId)
                .orElse(null);
        }
        List<BalanceUpdatedEvent.AccountBalanceSnapshot> snaps = new ArrayList<>();
        if (command != null && command.getDetails() != null) {
            for (BalanceExecutionResultCommand.CommandDetail d : command.getDetails()) {
                if (d.getAccount() == null || d.getAccount().getId() == null) {
                    continue;
                }
                Account a = accountRepository.findById(d.getAccount().getId()).orElse(d.getAccount());
                snaps.add(BalanceUpdatedEvent.AccountBalanceSnapshot.builder()
                    .accountId(a.getId())
                    .fullNumber(a.getFullNumber())
                    .currency(a.getCurrency())
                    .ledgerBalance(a.getLedgerBalance())
                    .availableBalance(a.getAvailableBalance())
                    .allowNegative(a.isAllowNegative())
                    .build());
            }
        }
        return BalanceUpdatedEvent.builder()
            .eventName("LEDGER_BALANCE_UPDATED")
            .movementId(movement.getId())
            .movementKey(movement.getMovementKey())
            .walletId(movement.getWalletId())
            .ownerId(ownerId)
            .orderType(movement.getOrderType())
            .amount(movement.getAmount())
            .currency(movement.getCurrency())
            .description(movement.getMetadata())
            .accounts(snaps)
            .build();
    }

    private Account resolveAccount(String idOrWalletRef, Currency currency) {
        if (idOrWalletRef == null || idOrWalletRef.isBlank()) {
            throw new BizException(AccountErrorResponse.ACC0400, "Account/wallet reference required");
        }
        try {
            Long id = Long.valueOf(idOrWalletRef);
            // Per-event COA posts with accountId — prefer the account row over wallet-id collision.
            Optional<Account> asAccount = accountRepository.findById(id);
            if (asAccount.isPresent()) {
                Account a = asAccount.get();
                if (currency != null && a.getCurrency() != null && a.getCurrency() != currency) {
                    throw new BizException(AccountErrorResponse.ACC0400,
                        "Account " + id + " currency " + a.getCurrency() + " != " + currency);
                }
                return a;
            }
            OptionalWalletAccount fromWallet = tryWallet(id, currency);
            if (fromWallet != null) {
                return fromWallet.account();
            }
            throw new BizException(AccountErrorResponse.ACC0404, "Account not found: " + id);
        } catch (NumberFormatException ex) {
            Wallet wallet = walletRepository.findByOwnerId(idOrWalletRef)
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                    "Wallet not found for " + idOrWalletRef));
            return accountForWalletCurrency(wallet, currency);
        }
    }

    private OptionalWalletAccount tryWallet(Long walletId, Currency currency) {
        return walletRepository.findById(walletId).map(w -> {
            Account a = accountForWalletCurrency(w, currency);
            return new OptionalWalletAccount(a);
        }).orElse(null);
    }

    private Account accountForWalletCurrency(Wallet wallet, Currency currency) {
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + wallet.getAccountId()));
        if (primary.getCurrency() == currency) {
            return primary;
        }
        return accountRepository.findFirstByMainAccountAndCurrency(primary.getMainAccount(), currency)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Account currency not found for wallet " + wallet.getId() + " / " + currency));
    }

    private record OptionalWalletAccount(Account account) {}
}
