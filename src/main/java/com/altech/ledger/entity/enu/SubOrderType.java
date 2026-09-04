package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/** domain SubOrderType (transfer variants). */
public enum SubOrderType {
    IN_WALLET,
    SWIFT;

    public static SubOrderType get(String input) {
        if (input != null) {
            for (SubOrderType value : SubOrderType.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(SubOrderType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
