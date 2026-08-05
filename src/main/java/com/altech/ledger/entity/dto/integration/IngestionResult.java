package com.altech.ledger.entity.dto.integration;

import com.altech.ledger.usecase.integration.TransactionRuleEngine;
import java.math.BigDecimal;
import java.util.UUID;

public record IngestionResult(
    String eventId,
    Status status,
    String operation,
    String reason,
    BigDecimal points,
    UUID transactionId,
    String walletExternalReference
) {
    public enum Status { EARNED, BURNED, PROCESSED, SKIPPED, DUPLICATE, ERROR }

    public static IngestionResult skipped(String eventId, String reason) {
        return new IngestionResult(eventId, Status.SKIPPED, null, reason, null, null, null);
    }

    public static IngestionResult applied(String eventId, TransactionRuleEngine.Operation operation,
                                          BigDecimal points, UUID transactionId, String walletRef) {
        Status status = switch (operation) {
            case EARN -> Status.EARNED;
            case BURN -> Status.BURNED;
            case PROCESS -> Status.PROCESSED;
        };
        return new IngestionResult(eventId, status, operation.name(), null, points, transactionId, walletRef);
    }

    public static IngestionResult duplicate(String eventId, TransactionRuleEngine.Operation operation,
                                            UUID transactionId, BigDecimal points, String walletRef) {
        Status status = switch (operation) {
            case EARN -> Status.DUPLICATE;
            case BURN -> Status.DUPLICATE;
            case PROCESS -> Status.DUPLICATE;
        };
        return new IngestionResult(eventId, status, operation.name(), "Already processed",
            points, transactionId, walletRef);
    }
}
