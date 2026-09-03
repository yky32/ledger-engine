package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/** WalletStatus. */
public enum WalletStatus {
    PENDING(false),
    VERIFIED(false),
    ACTIVE(true),
    DORMANT(true),
    CLOSED(true),
    SUSPENDED(true);

    private final boolean isFinal;

    WalletStatus(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public static WalletStatus get(String input) {
        if (input != null) {
            for (WalletStatus value : WalletStatus.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(WalletStatus.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
