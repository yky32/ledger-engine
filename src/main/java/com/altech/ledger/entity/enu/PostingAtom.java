package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/**
 * Atomic posting steps for UA-style use-case recipes.
 * COA Entity/Type/Sub stay internal — atoms resolve to account roles + {@link PostingIntent}.
 */
public enum PostingAtom {
    /** Credit member reward book in spend/result ccy (sheet: Transaction → HKD / LP). */
    CREDIT_REWARD,
    /** Debit member reward (redeem). */
    REDEEM,
    /** Debit member reward for cashback (phase-1 same books as redeem; payout rail later). */
    CASHBACK,
    /**
     * Convert member HKD reward → LP (phase-1: burn HKD points + earn LP same amount).
     * FX policy later.
     */
    CONVERT_HKD_TO_LP;

    public static PostingAtom get(String input) {
        if (input != null) {
            for (PostingAtom value : PostingAtom.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(PostingAtom.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
