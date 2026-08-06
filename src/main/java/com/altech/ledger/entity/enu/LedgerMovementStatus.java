package com.altech.ledger.entity.enu;

/** Port of payment-gateway {@code LedgerMovementStatus}. */
public enum LedgerMovementStatus {
    PROCESSING,
    PENDING_DOCS,
    REQUEST_FURTHER_INFORMATION,
    SETTLED,
    REJECTED,
    VOIDED_BY_ASSESSMENT,
    REFUNDED,
    ERROR,
    ALL,
    // engine extensions
    PENDING,
    REVERSED
}
