package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

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
    REVERSED;

    public static LedgerMovementStatus get(String input) {
        if (input != null) {
            for (LedgerMovementStatus value : LedgerMovementStatus.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(LedgerMovementStatus.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
