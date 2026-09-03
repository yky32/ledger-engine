package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/** WalletAssociationType. */
public enum WalletAssociationType {
    CUSTODIAN,
    CRYPTO;

    public static WalletAssociationType get(String input) {
        if (input != null) {
            for (WalletAssociationType value : WalletAssociationType.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(WalletAssociationType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
