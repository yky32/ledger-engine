package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/**
 * How to measure a wallet for banding.
 */
public enum WalletTierCriterion {
    /** Sum of {@code account.ledgerBalance} for this wallet in the policy currency. */
    LEDGER_BALANCE,
    ;

    public static WalletTierCriterion get(String input) {
        if (input != null) {
            for (WalletTierCriterion value : WalletTierCriterion.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s",
            input, input, Arrays.asList(WalletTierCriterion.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
