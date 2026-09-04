package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/** WalletType. */
public enum WalletType {
    INDIVIDUAL,
    CORPORATE;

    public static WalletType get(String input) {
        if (input != null) {
            for (WalletType value : WalletType.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(WalletType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
