package com.altech.ledger.usecase.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.integration.IngestionResult;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.integration.FailedTransactionIngest;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.FailedTransactionIngestRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.usecase.ledger.LedgerMovementShooter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Loyalty / transactional event ingest with eligibility gates.
 * <p>
 * Flow: gates → resolve/auto-create wallet (optional) → earn/process/burn in same TX.<br>
 * Failures / skips are persisted to {@code failed_transaction_ingest} first, then returned as SKIPPED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestTransactionUseCase {
    private final IntegrationProperties integrationProperties;
    private final TransactionRuleEngine transactionRuleEngine;
    private final EnsureWalletForIngestUseCase ensureWalletForIngestUseCase;
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerMovementShooter ledgerMovementShooter;
    private final FailedTransactionIngestRepository failedTransactionIngestRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public IngestionResult execute(TransactionalEvent event) {
        if (!integrationProperties.isEnabled()) {
            return _fail(event, "DISABLED", "Integration disabled");
        }

        // 1) eligibility gates first — do not auto-create wallet for bad events
        TransactionRuleEngine.EvaluationOutcome outcome = transactionRuleEngine.evaluate(event);
        if (!outcome.matched()) {
            return _fail(event,
                outcome.skipReasonCode() == null ? "NO_RULE" : outcome.skipReasonCode(),
                outcome.skipReason() == null ? "No matching rule" : outcome.skipReason());
        }

        TransactionRuleEngine.RuleDecision rule = outcome.decision().orElseThrow();
        Currency pointCurrency = Currency.get(rule.pointCurrency());
        String custId = event.associatedIdentifier();

        // 2) wallet exists? else upsert create (HKD+LP defaults) then continue
        EnsureWalletForIngestUseCase.ResolveResult resolved;
        try {
            resolved = ensureWalletForIngestUseCase.resolveOrProvision(custId, pointCurrency);
        } catch (RuntimeException ex) {
            log.error("wallet resolve/provision failed custId={}", custId, ex);
            return _fail(event, "WALLET_PROVISION",
                ex.getMessage() == null ? "wallet provision failed" : ex.getMessage());
        }
        if (resolved == null) {
            return _fail(event, "NO_WALLET",
                "Wallet not onboarded and auto-create disabled: " + custId);
        }
        Wallet wallet = resolved.wallet();
        String walletKey = custId;

        if (rule.operation() == TransactionRuleEngine.Operation.PROCESS) {
            String processType = rule.processType() == null ? "UNSPECIFIED" : rule.processType().toUpperCase();
            if (!"ADJUST".equals(processType)) {
                return _fail(event, "PROCESS_TYPE", "Process type not implemented: " + processType);
            }
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
            return IngestionResult.duplicate(event.eventId(), rule.operation(), id, rule.points(), walletKey);
        }

        // 3) earn / burn / process
        try {
            GetLedgerMovementResponseDto applied = ledgerMovementShooter.doEarnBurn(
                wallet.getId(),
                orderType,
                rule.points(),
                pointCurrency,
                movementKey,
                rule.operation() + " from " + event.eventType() + " (" + rule.formula() + ")"
                    + (resolved.provisioned() ? " [wallet-auto]" : "")
            );

            UUID txnId = applied.id() == null ? null
                : UUID.nameUUIDFromBytes(("movement:" + applied.id()).getBytes());
            return IngestionResult.applied(event.eventId(), rule.operation(), rule.points(), txnId, walletKey);
        } catch (RuntimeException ex) {
            log.error("ingest apply failed eventId={}", event.eventId(), ex);
            return _fail(event, "ERROR", ex.getMessage() == null ? "apply failed" : ex.getMessage());
        }
    }

    private IngestionResult _fail(TransactionalEvent event, String code, String reason) {
        _persistFailure(event, code, reason);
        return IngestionResult.skipped(event.eventId(), reason);
    }

    private void _persistFailure(TransactionalEvent event, String code, String reason) {
        try {
            FailedTransactionIngest row = new FailedTransactionIngest();
            row.setEventId(event.eventId());
            row.setAssociatedIdentifier(event.associatedIdentifier());
            row.setEventType(event.eventType());
            row.setAmount(event.amount());
            row.setCurrency(event.currency() == null ? null : event.currency().getIsoCode());
            row.setOccurredAt(event.occurredAt());
            row.setFailureCode(code);
            row.setReason(reason == null ? code : (reason.length() > 500 ? reason.substring(0, 500) : reason));
            row.setStatus("OPEN");
            try {
                row.setRawPayload(objectMapper.writeValueAsString(event));
            } catch (JsonProcessingException e) {
                row.setRawPayload(null);
            }
            failedTransactionIngestRepository.save(row);
        } catch (RuntimeException ex) {
            log.error("failed to persist failed_transaction_ingest eventId={}", event.eventId(), ex);
        }
    }
}
