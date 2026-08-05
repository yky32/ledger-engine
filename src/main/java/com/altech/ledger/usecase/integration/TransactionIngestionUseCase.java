package com.altech.ledger.usecase.integration;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.integration.IngestionResult;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.entity.dto.ledger.LedgerDto.*;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import com.altech.ledger.entity.po.JournalEntry;
import com.altech.ledger.entity.po.LedgerAccount;
import com.altech.ledger.repository.LedgerAccountRepository;
import com.altech.ledger.usecase.wallet.WalletOnboardingUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionIngestionUseCase {
    private final IntegrationProperties properties;
    private final TransactionRuleEngine ruleEngine;
    private final LedgerUseCase ledgerUseCase;
    private final LedgerAccountRepository accounts;
    private final WalletOnboardingUseCase walletOnboardingUseCase;

    public TransactionIngestionUseCase(IntegrationProperties properties, TransactionRuleEngine ruleEngine,
                                       LedgerUseCase ledgerUseCase, LedgerAccountRepository accounts,
                                       WalletOnboardingUseCase walletOnboardingUseCase) {
        this.properties = properties;
        this.ruleEngine = ruleEngine;
        this.ledgerUseCase = ledgerUseCase;
        this.accounts = accounts;
        this.walletOnboardingUseCase = walletOnboardingUseCase;
    }

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
        Optional<LedgerAccount> wallet = accounts.findByExternalReference(walletRef);
        if (wallet.isEmpty()) {
            return IngestionResult.skipped(event.eventId(), "Wallet not onboarded: " + walletRef);
        }

        if (rule.operation() == TransactionRuleEngine.Operation.PROCESS) {
            return processOperation(event, rule, wallet.get(), walletRef);
        }

        LedgerAccount counterparty = counterpartyAccount(rule);
        if (counterparty == null) {
            return IngestionResult.skipped(event.eventId(), "Program pool account missing for " + rule.pointCurrency());
        }

        List<EntryRequest> legs = switch (rule.operation()) {
            case EARN -> List.of(
                entry(counterparty.getId(), JournalEntry.Side.DEBIT, rule.points(), rule.pointCurrency(), 1),
                entry(wallet.get().getId(), JournalEntry.Side.CREDIT, rule.points(), rule.pointCurrency(), 2));
            case BURN -> List.of(
                entry(wallet.get().getId(), JournalEntry.Side.DEBIT, rule.points(), rule.pointCurrency(), 1),
                entry(counterparty.getId(), JournalEntry.Side.CREDIT, rule.points(), rule.pointCurrency(), 2));
            case PROCESS -> List.of();
        };

        return post(event, rule, walletRef, legs);
    }

    private IngestionResult processOperation(TransactionalEvent event, TransactionRuleEngine.RuleDecision rule,
                                             LedgerAccount wallet, String walletRef) {
        String processType = rule.processType() == null ? "UNSPECIFIED" : rule.processType().toUpperCase();
        return switch (processType) {
            case "ADJUST" -> post(event, rule, walletRef, List.of(
                entry(wallet.getId(), JournalEntry.Side.CREDIT, rule.points(), rule.pointCurrency(), 1),
                entry(requirePool(properties.getExpensePoolRefTemplate(), rule.pointCurrency()).getId(),
                    JournalEntry.Side.DEBIT, rule.points(), rule.pointCurrency(), 2)));
            default -> IngestionResult.skipped(event.eventId(),
                "Process type not implemented: " + processType);
        };
    }

    private IngestionResult post(TransactionalEvent event, TransactionRuleEngine.RuleDecision rule,
                                 String walletRef, List<EntryRequest> legs) {
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
            return IngestionResult.applied(event.eventId(), rule.operation(), rule.points(),
                result.transaction().id(), walletRef);
        }
        return IngestionResult.duplicate(event.eventId(), rule.operation(), result.transaction().id(),
            rule.points(), walletRef);
    }

    private LedgerAccount counterpartyAccount(TransactionRuleEngine.RuleDecision rule) {
        return switch (rule.operation()) {
            case EARN -> requirePool(properties.getExpensePoolRefTemplate(), rule.pointCurrency());
            case BURN -> requirePool(properties.getLiabilityPoolRefTemplate(), rule.pointCurrency());
            case PROCESS -> null;
        };
    }

    private LedgerAccount requirePool(String template, String currency) {
        String ref = template.replace("{currency}", currency);
        return accounts.findByExternalReference(ref).orElse(null);
    }

    private EntryRequest entry(java.util.UUID accountId, JournalEntry.Side side,
                             java.math.BigDecimal amount, String currency, int sequence) {
        return new EntryRequest(accountId, side, amount, currency, sequence);
    }
}
