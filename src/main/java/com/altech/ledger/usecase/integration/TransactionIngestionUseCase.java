package com.altech.ledger.usecase.integration;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.integration.IngestionResult;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.entity.dto.ledger.LedgerDto.*;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.journal.JournalEntry;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import com.altech.ledger.usecase.wallet.WalletOnboardingUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionIngestionUseCase {
    private final IntegrationProperties properties;
    private final TransactionRuleEngine ruleEngine;
    private final LedgerUseCase ledgerUseCase;
    private final AccountRepository accounts;
    private final WalletRepository wallets;
    private final LedgerMovementRepository movements;
    private final WalletOnboardingUseCase walletOnboardingUseCase;

    @Transactional
    public IngestionResult ingest(TransactionalEvent event) {
        if (!properties.isEnabled()) {
            return IngestionResult.skipped(event.eventId(), "Integration disabled");
        }

        Optional<TransactionRuleEngine.RuleDecision> decision = ruleEngine.evaluate(event);
        if (decision.isEmpty()) {
            return IngestionResult.skipped(event.eventId(), "No matching rule");
        }

        TransactionRuleEngine.RuleDecision rule = decision.get();
        String walletRef = walletOnboardingUseCase.walletRef(event.userId(), rule.pointCurrency());
        Optional<Account> walletAccount = accounts.findByFullNumber(walletRef);
        if (walletAccount.isEmpty()) {
            return IngestionResult.skipped(event.eventId(), "Wallet not onboarded: " + walletRef);
        }

        if (rule.operation() == TransactionRuleEngine.Operation.PROCESS) {
            return processOperation(event, rule, walletAccount.get(), walletRef);
        }

        Account counterparty = counterpartyAccount(rule);
        if (counterparty == null) {
            return IngestionResult.skipped(event.eventId(), "Program pool account missing for " + rule.pointCurrency());
        }

        List<EntryRequest> legs = switch (rule.operation()) {
            case EARN -> List.of(
                entry(counterparty.getId(), JournalEntry.Side.DEBIT, rule.points(), rule.pointCurrency(), 1),
                entry(walletAccount.get().getId(), JournalEntry.Side.CREDIT, rule.points(), rule.pointCurrency(), 2));
            case BURN -> List.of(
                entry(walletAccount.get().getId(), JournalEntry.Side.DEBIT, rule.points(), rule.pointCurrency(), 1),
                entry(counterparty.getId(), JournalEntry.Side.CREDIT, rule.points(), rule.pointCurrency(), 2));
            case PROCESS -> List.of();
        };

        return post(event, rule, walletRef, walletAccount.get(), legs);
    }

    private IngestionResult processOperation(TransactionalEvent event, TransactionRuleEngine.RuleDecision rule,
                                             Account wallet, String walletRef) {
        String processType = rule.processType() == null ? "UNSPECIFIED" : rule.processType().toUpperCase();
        return switch (processType) {
            case "ADJUST" -> post(event, rule, walletRef, wallet, List.of(
                entry(wallet.getId(), JournalEntry.Side.CREDIT, rule.points(), rule.pointCurrency(), 1),
                entry(requirePool(properties.getExpensePoolRefTemplate(), rule.pointCurrency()).getId(),
                    JournalEntry.Side.DEBIT, rule.points(), rule.pointCurrency(), 2)));
            default -> IngestionResult.skipped(event.eventId(),
                "Process type not implemented: " + processType);
        };
    }

    private IngestionResult post(TransactionalEvent event, TransactionRuleEngine.RuleDecision rule,
                                 String walletRef, Account walletAccount, List<EntryRequest> legs) {
        String idempotencyKey = rule.operation().name().toLowerCase() + "-event:" + event.eventId();
        PostTransactionRequest request = new PostTransactionRequest(
            idempotencyKey,
            event.eventId(),
            rule.operation() + " from " + event.eventType() + " (" + rule.formula() + ")",
            event.occurredAt(),
            legs
        );
        LedgerUseCase.PostingResult result = ledgerUseCase.post(request);
        if (result.created()) {
            logMovement(event, rule, walletAccount, result.transaction().id());
            return IngestionResult.applied(event.eventId(), rule.operation(), rule.points(),
                result.transaction().id(), walletRef);
        }
        return IngestionResult.duplicate(event.eventId(), rule.operation(), result.transaction().id(),
            rule.points(), walletRef);
    }

    /**
     * Also write LedgerMovement (the-wallet-ledger business log) without re-applying balances
     * (journal post already dual-wrote Account balances).
     */
    private void logMovement(TransactionalEvent event, TransactionRuleEngine.RuleDecision rule,
                             Account walletAccount, UUID journalTxnId) {
        String key = "loyalty-" + rule.operation().name().toLowerCase() + "-" + event.eventId();
        if (movements.findByMovementKey(key).isPresent()) {
            return;
        }
        OrderType orderType = switch (rule.operation()) {
            case EARN -> OrderType.EARN;
            case BURN -> OrderType.BURN;
            case PROCESS -> OrderType.PROCESS;
        };
        Long walletId = wallets.findByAccountId(walletAccount.getId()).map(Wallet::getId).orElse(null);
        if (walletId == null) {
            // loyalty onboard may only create Account without Wallet row in old path;
            // try owner lookup
            walletId = wallets.findByOwnerIdAndCurrency(event.userId(), rule.pointCurrency())
                .map(Wallet::getId).orElse(null);
        }
        if (walletId == null) {
            return;
        }
        String origin = orderType == OrderType.BURN ? String.valueOf(walletId) : null;
        String target = orderType == OrderType.EARN || orderType == OrderType.PROCESS
            ? String.valueOf(walletId) : null;
        LedgerMovement movement = new LedgerMovement(key, walletId, orderType, LedgerMovementMode.AUTO,
            origin, target, rule.points(), rule.pointCurrency(),
            rule.operation() + " " + event.eventType());
        movement.markSettled(journalTxnId);
        movements.save(movement);
    }

    private Account counterpartyAccount(TransactionRuleEngine.RuleDecision rule) {
        return switch (rule.operation()) {
            case EARN -> requirePool(properties.getExpensePoolRefTemplate(), rule.pointCurrency());
            case BURN -> requirePool(properties.getLiabilityPoolRefTemplate(), rule.pointCurrency());
            case PROCESS -> null;
        };
    }

    private Account requirePool(String template, String currency) {
        String ref = template.replace("{currency}", currency);
        return accounts.findByFullNumber(ref).orElse(null);
    }

    private EntryRequest entry(Long accountId, JournalEntry.Side side,
                               java.math.BigDecimal amount, String currency, int sequence) {
        return new EntryRequest(accountId, side, amount, currency, sequence);
    }
}
