package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Which balances an earn/burn (or reverse) writes.
 * Omit / blank → {@link #BOTH} (ledger and available, today's default).
 */
public enum ApplyTo {
    /** ledgerBalance and availableBalance (default). */
    BOTH,
    /** ledgerBalance only — AUTH / pending. */
    LEDGER,
    /** availableBalance only — POST after AUTH; cannot exceed ledger. */
    AVAILABLE,
    ;

    @JsonValue
    public String json() {
        return name();
    }

    public boolean affectsLedger() {
        return this == BOTH || this == LEDGER;
    }

    public boolean affectsAvailable() {
        return this == BOTH || this == AVAILABLE;
    }

    /**
     * Map ADD/SUBTRACT to the op for this applyTo. HOLD_LOCK / HOLD_UNLOCK unchanged.
     */
    public BalanceOperation remap(BalanceOperation op) {
        if (op == null || this == BOTH) {
            return op;
        }
        if (op == BalanceOperation.HOLD_LOCK || op == BalanceOperation.HOLD_UNLOCK) {
            return op;
        }
        if (this == LEDGER) {
            if (op == BalanceOperation.ADD) {
                return BalanceOperation.LEDGER_ADD;
            }
            if (op == BalanceOperation.SUBTRACT) {
                return BalanceOperation.LEDGER_SUB;
            }
        }
        if (this == AVAILABLE) {
            if (op == BalanceOperation.ADD) {
                return BalanceOperation.HOLD_UNLOCK;
            }
            if (op == BalanceOperation.SUBTRACT) {
                return BalanceOperation.HOLD_LOCK;
            }
        }
        return op;
    }

    /** Blank / omit → BOTH. Unknown token → {@code PAM0400}. */
    @JsonCreator
    public static ApplyTo get(String input) {
        if (input == null || input.isBlank()) {
            return BOTH;
        }
        String t = input.trim();
        if ("ALL".equalsIgnoreCase(t) || "BOTH_BALANCES".equalsIgnoreCase(t)) {
            return BOTH;
        }
        if ("LEDGER_ONLY".equalsIgnoreCase(t) || "LEDGER_BALANCE".equalsIgnoreCase(t)) {
            return LEDGER;
        }
        if ("AVAILABLE_ONLY".equalsIgnoreCase(t) || "AVAIL".equalsIgnoreCase(t)
            || "AVAILABLE_BALANCE".equalsIgnoreCase(t)) {
            return AVAILABLE;
        }
        for (ApplyTo value : values()) {
            if (t.equalsIgnoreCase(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s",
            input, input, Arrays.asList(values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
