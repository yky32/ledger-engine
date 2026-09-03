package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/**
 * High-level posting intent — maps to {@link OrderType} for execution.
 * <p>
 * Money rails and loyalty share one balance engine; intents stay distinct
 * so DEPOSIT ≠ EARN (PROGRAM double-entry).
 */
public enum PostingIntent {
    /** Single-sided credit member book (external funding). */
    DEPOSIT,
    /** Single-sided debit member book (external payout). */
    WITHDRAWAL,
    /** Debit one wallet / credit another (same ccy). */
    IN_WALLET_TRANSFER,
    /** PROGRAM pool → member (loyalty earn). */
    EARN,
    /** Member → PROGRAM pool (loyalty burn). */
    BURN,
    /** Lock available only. */
    HOLD,
    /** Unlock available only. */
    RELEASE;

    public OrderType toOrderType() {
        return switch (this) {
            case DEPOSIT -> OrderType.DEPOSIT;
            case WITHDRAWAL -> OrderType.WITHDRAWAL;
            case IN_WALLET_TRANSFER -> OrderType.IN_WALLET_TRANSFER;
            case EARN -> OrderType.EARN;
            case BURN -> OrderType.BURN;
            case HOLD -> OrderType.HOLD;
            case RELEASE -> OrderType.RELEASE;
        };
    }

    public static PostingIntent get(String input) {
        if (input != null) {
            for (PostingIntent value : PostingIntent.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(PostingIntent.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
