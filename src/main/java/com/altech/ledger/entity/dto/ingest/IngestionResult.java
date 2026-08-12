package com.altech.ledger.entity.dto.ingest;

import com.altech.ledger.usecase.digestion.TransactionRuleEngine;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Outcome of processing a {@link TransactionalEvent}: applied, skipped, or duplicate.
 * Applied/duplicate include {@link #legs} when double-entry entries exist.
 */
public record IngestionResult(
    String eventId,
    Status status,
    String operation,
    String reason,
    BigDecimal points,
    UUID transactionId,
    String walletExternalReference,
    Long movementId,
    List<LedgerLegDto> legs
) {
    public enum Status { EARNED, BURNED, PROCESSED, SKIPPED, DUPLICATE, ERROR }

    public static IngestionResult skipped(String eventId, String reason) {
        return new IngestionResult(eventId, Status.SKIPPED, null, reason, null, null, null, null, List.of());
    }

    public static IngestionResult applied(
        String eventId,
        TransactionRuleEngine.Operation operation,
        BigDecimal points,
        UUID transactionId,
        String walletRef,
        Long movementId,
        List<LedgerLegDto> legs
    ) {
        Status status = switch (operation) {
            case EARN -> Status.EARNED;
            case BURN -> Status.BURNED;
            case PROCESS -> Status.PROCESSED;
        };
        return new IngestionResult(
            eventId, status, operation.name(), null, points, transactionId, walletRef,
            movementId, legs == null ? List.of() : List.copyOf(legs));
    }

    public static IngestionResult duplicate(
        String eventId,
        TransactionRuleEngine.Operation operation,
        UUID transactionId,
        BigDecimal points,
        String walletRef,
        Long movementId,
        List<LedgerLegDto> legs
    ) {
        return new IngestionResult(
            eventId, Status.DUPLICATE, operation.name(), "Already processed",
            points, transactionId, walletRef, movementId, legs == null ? List.of() : List.copyOf(legs));
    }
}
