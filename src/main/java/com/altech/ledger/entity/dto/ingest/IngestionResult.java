package com.altech.ledger.entity.dto.ingest;

import com.altech.ledger.usecase.digestion.TransactionRuleEngine;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Outcome of processing a {@link TransactionalEvent}: applied, skipped, duplicate, or dry-run preview.
 * Trust pack B: {@link #matchedRuleCode} + {@link #eligibilityTrace}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IngestionResult(
    String eventId,
    Status status,
    String operation,
    String reason,
    BigDecimal points,
    UUID transactionId,
    String walletExternalReference,
    Long movementId,
    List<LedgerLegDto> legs,
    /** Winning DigestionRule.code when matched. */
    String matchedRuleCode,
    /** Candidate rules tried (same eventType); empty if disabled before Brain. */
    List<EligibilityTraceEntry> eligibilityTrace,
    /** True when POST …/dry-run (no wallet / no books). */
    Boolean dryRun
) {
    public enum Status { EARNED, BURNED, PROCESSED, SKIPPED, DUPLICATE, ERROR }

    public static IngestionResult skipped(
        String eventId,
        String reason,
        List<EligibilityTraceEntry> trace
    ) {
        return new IngestionResult(
            eventId, Status.SKIPPED, null, reason, null, null, null, null,
            List.of(), null, copyTrace(trace), null);
    }

    public static IngestionResult skipped(String eventId, String reason) {
        return skipped(eventId, reason, List.of());
    }

    public static IngestionResult applied(
        String eventId,
        TransactionRuleEngine.Operation operation,
        BigDecimal points,
        UUID transactionId,
        String walletRef,
        Long movementId,
        List<LedgerLegDto> legs,
        String matchedRuleCode,
        List<EligibilityTraceEntry> trace
    ) {
        Status status = switch (operation) {
            case EARN -> Status.EARNED;
            case BURN -> Status.BURNED;
            case PROCESS -> Status.PROCESSED;
        };
        return new IngestionResult(
            eventId, status, operation.name(), null, points, transactionId, walletRef,
            movementId, legs == null ? List.of() : List.copyOf(legs),
            matchedRuleCode, copyTrace(trace), null);
    }

    public static IngestionResult duplicate(
        String eventId,
        TransactionRuleEngine.Operation operation,
        UUID transactionId,
        BigDecimal points,
        String walletRef,
        Long movementId,
        List<LedgerLegDto> legs,
        String matchedRuleCode,
        List<EligibilityTraceEntry> trace
    ) {
        return new IngestionResult(
            eventId, Status.DUPLICATE, operation.name(), "Already processed",
            points, transactionId, walletRef, movementId,
            legs == null ? List.of() : List.copyOf(legs),
            matchedRuleCode, copyTrace(trace), null);
    }

    /** Preview only — status as if earned/burned but no side effects. */
    public static IngestionResult preview(
        String eventId,
        TransactionRuleEngine.Operation operation,
        BigDecimal points,
        String matchedRuleCode,
        List<EligibilityTraceEntry> trace
    ) {
        Status status = switch (operation) {
            case EARN -> Status.EARNED;
            case BURN -> Status.BURNED;
            case PROCESS -> Status.PROCESSED;
        };
        return new IngestionResult(
            eventId, status, operation.name(), null, points, null, null, null,
            List.of(), matchedRuleCode, copyTrace(trace), Boolean.TRUE);
    }

    public static IngestionResult previewSkipped(
        String eventId,
        String reason,
        List<EligibilityTraceEntry> trace
    ) {
        return new IngestionResult(
            eventId, Status.SKIPPED, null, reason, null, null, null, null,
            List.of(), null, copyTrace(trace), Boolean.TRUE);
    }

    private static List<EligibilityTraceEntry> copyTrace(List<EligibilityTraceEntry> trace) {
        return trace == null || trace.isEmpty() ? List.of() : List.copyOf(trace);
    }
}
