package com.altech.ledger.usecase.integration;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.integration.IngestionResult;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.ledger.LedgerMovementShooter;
import com.altech.ledger.usecase.wallet.WalletOnboardingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Loyalty / transactional event ingest. Applies balances via movement execution
 * (no classic journal layer).
 */
@Service
@RequiredArgsConstructor
public class TransactionIngestionUseCase {
    private final IntegrationProperties properties;
    private final TransactionRuleEngine ruleEngine;
    private final AccountRepository accounts;
    private final WalletRepository wallets;
    private final LedgerMovementRepository movements;
    private final WalletOnboardingUseCase walletOnboardingUseCase;
    private final LedgerMovementShooter shooter;

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
            String processType = rule.processType() == null ? "UNSPECIFIED" : rule.processType().toUpperCase();
            if (!"ADJUST".equals(processType)) {
                return IngestionResult.skipped(event.eventId(), "Process type not implemented: " + processType);
            }
        }

        Optional<Wallet> wallet = wallets.findByAccountId(walletAccount.get().getId())
            .or(() -> wallets.findByOwnerIdAndCurrency(event.userId(), rule.pointCurrency()));
        if (wallet.isEmpty()) {
            return IngestionResult.skipped(event.eventId(), "Wallet row missing for " + walletRef);
        }

        OrderType orderType = switch (rule.operation()) {
            case EARN, PROCESS -> OrderType.EARN;
            case BURN -> OrderType.BURN;
        };
        String movementKey = "loyalty-" + rule.operation().name().toLowerCase() + "-" + event.eventId();

        Optional<LedgerMovement> existing = movements.findByMovementKey(movementKey);
        if (existing.isPresent()) {
            UUID id = existing.get().getId() == null ? null
                : UUID.nameUUIDFromBytes(("movement:" + existing.get().getId()).getBytes());
            return IngestionResult.duplicate(event.eventId(), rule.operation(), id, rule.points(), walletRef);
        }

        MovementResponse applied = shooter.doEarnBurn(
            wallet.get().getId(),
            orderType,
            rule.points(),
            rule.pointCurrency(),
            movementKey,
            rule.operation() + " from " + event.eventType() + " (" + rule.formula() + ")"
        );

        UUID txnId = applied.id() == null ? null
            : UUID.nameUUIDFromBytes(("movement:" + applied.id()).getBytes());
        return IngestionResult.applied(event.eventId(), rule.operation(), rule.points(), txnId, walletRef);
    }
}
