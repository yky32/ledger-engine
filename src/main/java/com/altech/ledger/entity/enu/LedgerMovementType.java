package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/** LedgerMovementType — . */
public enum LedgerMovementType {
    CHARGE,
    TRANSFER;

    public static LedgerMovementType get(String input) {
        if (input != null) {
            for (LedgerMovementType value : LedgerMovementType.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(LedgerMovementType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
