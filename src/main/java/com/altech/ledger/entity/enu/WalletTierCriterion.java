package com.altech.ledger.entity.enu;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How to measure the wallet for banding. v1: stock on the watched COA book.
 */
public enum WalletTierCriterion {
    /** Amount total = {@code account.ledgerBalance} on the configured book. */
    LEDGER_BALANCE;

    @JsonValue
    public String json() {
        return name();
    }

    @JsonCreator
    public static WalletTierCriterion from(String raw) {
        if (raw == null || raw.isBlank()) {
            return LEDGER_BALANCE;
        }
        String t = raw.trim().toUpperCase();
        if ("LEDGER_BALANCE".equals(t) || "AMOUNT_TOTAL".equals(t) || "BALANCE".equals(t)) {
            return LEDGER_BALANCE;
        }
        throw new IllegalArgumentException("Unsupported wallet tier criterion: " + raw);
    }
}
