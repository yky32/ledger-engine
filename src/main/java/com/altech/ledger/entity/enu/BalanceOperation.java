package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

public enum BalanceOperation {
    ADD,
    SUBTRACT,
    /** Reduce available only (ledger unchanged) — HOLD. */
    HOLD_LOCK,
    /** Restore available only (ledger unchanged) — RELEASE. */
    HOLD_UNLOCK;

    public static BalanceOperation get(String input) {
        if (input != null) {
            for (BalanceOperation value : BalanceOperation.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(BalanceOperation.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
