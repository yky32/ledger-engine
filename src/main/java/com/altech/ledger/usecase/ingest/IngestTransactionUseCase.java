package com.altech.ledger.usecase.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.ingest.IngestionResult;
import com.altech.ledger.entity.dto.ingest.LedgerLegDto;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ingest.FailedTransactionIngest;
import com.altech.ledger.entity.po.ingest.IngestPolicy;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerEntry;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.repository.FailedTransactionIngestRepository;
import com.altech.ledger.repository.LedgerEntryRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.usecase.digestion.TransactionRuleEngine;
import com.altech.ledger.usecase.ledger.LedgerMovementShooter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>Ingest</b> orchestration: run the full webhook pipeline in one TX.
 * <p>
 * Order:
 * <ol>
 *   <li>{@link IngestPolicy} — door (enabled?)</li>
 *   <li>{@link com.altech.ledger.usecase.digestion.TransactionRuleEngine} — digestion (match + points)</li>
 *   <li>Auto-wallet from ingest policy if needed</li>
 *   <li>Earn/burn double-entry legs</li>
 * </ol>
 * Digestion rules themselves are owned by {@code usecase.digestion}; this class only calls them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestTransactionUseCase {
    private final IngestPolicyUseCase ingestPolicyUseCase;
    private final TransactionRuleEngine transactionRuleEngine;
    private final EnsureWalletForIngestUseCase ensureWalletForIngestUseCase;
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerMovementShooter ledgerMovementShooter;
    private final FailedTransactionIngestRepository failedTransactionIngestRepository;
    private final ObjectMapper objectMapper;

    /** When true, SKIPPED paths do not insert a new failed_transaction_ingest row. */
    private static final ThreadLocal<Boolean> SUPPRESS_FAIL_PERSIST = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Run ingest without writing a new fail row (used by fail-replay). */
    public IngestionResult executeWithoutFailPersist(TransactionalEvent event) {
        SUPPRESS_FAIL_PERSIST.set(Boolean.TRUE);
        try {
            return execute(event);
        } finally {
            SUPPRESS_FAIL_PERSIST.remove();
        }
    }

    @Transactional
    public IngestionResult execute(TransactionalEvent event) {
        if (!Boolean.TRUE.equals(ingestPolicyUseCase.requireEffective().getIsEnabled())) {
            return _fail(event, "DISABLED", "Integration disabled");
        }

        TransactionRuleEngine.EvaluationOutcome outcome = transactionRuleEngine.evaluate(event);
        if (!outcome.matched()) {
            return _fail(event,
                outcome.skipReasonCode() == null ? "NO_RULE" : outcome.skipReasonCode(),
                outcome.skipReason() == null ? "No matching rule" : outcome.skipReason());
        }

        TransactionRuleEngine.RuleDecision rule = outcome.decision().orElseThrow();
        Currency pointCurrency = Currency.get(rule.pointCurrency());
        String custId = event.ownerId();

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
            LedgerMovement m = existing.get();
            UUID id = m.getId() == null ? null
                : UUID.nameUUIDFromBytes(("movement:" + m.getId()).getBytes());
            return IngestionResult.duplicate(
                event.eventId(), rule.operation(), id, rule.points(), walletKey,
                m.getId(), _legs(m.getId()));
        }

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
            return IngestionResult.applied(
                event.eventId(), rule.operation(), rule.points(), txnId, walletKey,
                applied.id(), _legs(applied.id()));
        } catch (RuntimeException ex) {
            log.error("ingest apply failed eventId={}", event.eventId(), ex);
            return _fail(event, "ERROR", ex.getMessage() == null ? "apply failed" : ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<LedgerLegDto> legsForMovementId(Long movementId) {
        if (movementId == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "movementId required");
        }
        return _legs(movementId);
    }

    @Transactional(readOnly = true)
    public List<LedgerLegDto> legsForEventId(String eventId, String operationHint) {
        List<String> keys = new ArrayList<>();
        if (operationHint != null && !operationHint.isBlank()) {
            keys.add("loyalty-" + operationHint.trim().toLowerCase() + "-" + eventId);
        } else {
            keys.add("loyalty-earn-" + eventId);
            keys.add("loyalty-burn-" + eventId);
            keys.add("loyalty-process-" + eventId);
        }
        for (String key : keys) {
            Optional<LedgerMovement> m = ledgerMovementRepository.findByMovementKey(key);
            if (m.isPresent()) {
                return _legs(m.get().getId());
            }
        }
        throw new BizException(MovementErrorResponse.MOV0404, "No movement for eventId=" + eventId);
    }

    private List<LedgerLegDto> _legs(Long movementId) {
        if (movementId == null) {
            return List.of();
        }
        List<LedgerLegDto> out = new ArrayList<>();
        for (LedgerEntry e : ledgerEntryRepository.findByTxnId(movementId)) {
            Long accountId = null;
            try {
                accountId = Long.valueOf(e.getTargetId());
            } catch (Exception ignored) {
                // leave null
            }
            out.add(new LedgerLegDto(e.getId(), accountId, e.getDirection(), e.getAmount(), e.getCurrency()));
        }
        return out;
    }

    private IngestionResult _fail(TransactionalEvent event, String code, String reason) {
        _persistFailure(event, code, reason);
        return IngestionResult.skipped(event.eventId(), reason);
    }

    private void _persistFailure(TransactionalEvent event, String code, String reason) {
        if (Boolean.TRUE.equals(SUPPRESS_FAIL_PERSIST.get())) {
            log.debug("suppress fail persist (replay) eventId={} code={}", event.eventId(), code);
            return;
        }
        try {
            FailedTransactionIngest row = new FailedTransactionIngest();
            row.setEventId(event.eventId());
            row.setOwnerId(event.ownerId());
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
