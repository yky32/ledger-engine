package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/** LedgerMovementMode. */
public enum LedgerMovementMode {
    AUTO,
    MANUAL,
    ALL;

    public static LedgerMovementMode get(String input) {
        if (input != null) {
            for (LedgerMovementMode value : LedgerMovementMode.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(LedgerMovementMode.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
