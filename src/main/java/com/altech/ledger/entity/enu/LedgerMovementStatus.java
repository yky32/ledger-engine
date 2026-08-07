package com.altech.ledger.entity.enu;

/** LedgerMovementStatus. */
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
