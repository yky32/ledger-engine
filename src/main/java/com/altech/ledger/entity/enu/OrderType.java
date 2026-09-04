package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/**
 * OrderType. extended with engine loyalty / transfer types.
 */
public enum OrderType {
    PAYMENT_LINK,
    WITHDRAWAL,
    WALLET_TRANSFER,
    DEPOSIT,
    ADJUSTMENT,
    ADJUSTMENT_REFUND,
    ADJUSTMENT_TOTAL,
    BANK_CHARGE,
    HANDLING_CHARGE,
    // engine extensions (new on top)
    IN_WALLET_TRANSFER,
    SWIFT_TRANSFER,
    EARN,
    BURN,
    PROCESS,
    CHARGE,
    /** Lock available without changing ledger. */
    HOLD,
    /** Unlock available previously held. */
    RELEASE;

    public static OrderType get(String input) {
        if (input != null) {
            for (OrderType value : OrderType.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(OrderType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
