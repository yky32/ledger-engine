package com.altech.ledger.usecase.integration;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.integration.IngestionResult;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.ledger.LedgerMovementShooter;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

/**
 * Loyalty / transactional event ingest. Applies balances via movement execution.
 */
@Component
@RequiredArgsConstructor
public class IngestTransactionUseCase {
    private final IntegrationProperties integrationProperties;
    private final TransactionRuleEngine transactionRuleEngine;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final LedgerMovementRepository ledgerMovementRepository;
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;
    private final LedgerMovementShooter ledgerMovementShooter;

    @Transactional
    public IngestionResult execute(TransactionalEvent event) {
        if (!integrationProperties.isEnabled()) {
            return IngestionResult.skipped(event.eventId(), "Integration disabled");
        }

        Optional<TransactionRuleEngine.RuleDecision> decision = transactionRuleEngine.evaluate(event);
        if (decision.isEmpty()) {
            return IngestionResult.skipped(event.eventId(), "No matching rule");
        }

        TransactionRuleEngine.RuleDecision rule = decision.get();
        String walletRef = createWalletOnboardingUseCase.walletRef(event.userId(), rule.pointCurrency());
        Optional<Account> walletAccount = accountRepository.findByFullNumber(walletRef);
        if (walletAccount.isEmpty()) {
            return IngestionResult.skipped(event.eventId(), "Wallet not onboarded: " + walletRef);
        }

        if (rule.operation() == TransactionRuleEngine.Operation.PROCESS) {
            String processType = rule.processType() == null ? "UNSPECIFIED" : rule.processType().toUpperCase();
            if (!"ADJUST".equals(processType)) {
                return IngestionResult.skipped(event.eventId(), "Process type not implemented: " + processType);
            }
        }

        Optional<Wallet> wallet = walletRepository.findByAccountId(walletAccount.get().getId())
            .or(() -> walletRepository.findByOwnerIdAndCurrency(event.userId(), rule.pointCurrency()));
        if (wallet.isEmpty()) {
            return IngestionResult.skipped(event.eventId(), "Wallet row missing for " + walletRef);
        }

        OrderType orderType = switch (rule.operation()) {
            case EARN, PROCESS -> OrderType.EARN;
            case BURN -> OrderType.BURN;
        };
        String movementKey = "loyalty-" + rule.operation().name().toLowerCase() + "-" + event.eventId();

        Optional<LedgerMovement> existing = ledgerMovementRepository.findByMovementKey(movementKey);
        if (existing.isPresent()) {
            UUID id = existing.get().getId() == null ? null
                : UUID.nameUUIDFromBytes(("movement:" + existing.get().getId()).getBytes());
            return IngestionResult.duplicate(event.eventId(), rule.operation(), id, rule.points(), walletRef);
        }

        GetLedgerMovementResponseDto applied = ledgerMovementShooter.doEarnBurn(
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
