package com.altech.ledger.entity.enu;

public enum BalanceOperation {
    ADD,
    SUBTRACT,
    /** Reduce available only (ledger unchanged) — HOLD. */
    HOLD_LOCK,
    /** Restore available only (ledger unchanged) — RELEASE. */
    HOLD_UNLOCK
}
