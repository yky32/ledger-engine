package com.altech.ledger.entity.dto.posting;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.PostingIntent;

import java.math.BigDecimal;

/**
 * Central posting command — one shape for money rails + loyalty + hold.
 * Prefer {@link com.altech.ledger.usecase.ledger.PostingService#post} over ad-hoc shooter calls.
 */
public record PostingCommand(
    PostingIntent intent,
    Long walletId,
    BigDecimal amount,
    Currency currency,
    String movementKey,
    String description,
    LedgerMovementMode mode,
    /** Transfer destination wallet id (IN_WALLET_TRANSFER). */
    Long counterpartyWalletId,
    /** Optional external / free-text party on withdrawal target. */
    String externalPartyId
) {
    public PostingCommand {
        if (mode == null) {
            mode = LedgerMovementMode.AUTO;
        }
    }

    public static PostingCommand deposit(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        LedgerMovementMode mode
    ) {
        return new PostingCommand(
            PostingIntent.DEPOSIT, walletId, amount, currency, movementKey, description, mode, null, null);
    }

    public static PostingCommand withdrawal(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        LedgerMovementMode mode,
        String externalPartyId
    ) {
        return new PostingCommand(
            PostingIntent.WITHDRAWAL, walletId, amount, currency, movementKey, description, mode,
            null, externalPartyId);
    }

    public static PostingCommand inWalletTransfer(
        Long fromWalletId,
        Long toWalletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        LedgerMovementMode mode
    ) {
        return new PostingCommand(
            PostingIntent.IN_WALLET_TRANSFER, fromWalletId, amount, currency, movementKey, description,
            mode, toWalletId, null);
    }

    public static PostingCommand earn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        return new PostingCommand(
            PostingIntent.EARN, walletId, amount, currency, movementKey, description,
            LedgerMovementMode.AUTO, null, null);
    }

    public static PostingCommand burn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        return new PostingCommand(
            PostingIntent.BURN, walletId, amount, currency, movementKey, description,
            LedgerMovementMode.AUTO, null, null);
    }

    public static PostingCommand hold(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        return new PostingCommand(
            PostingIntent.HOLD, walletId, amount, currency, movementKey, description,
            LedgerMovementMode.AUTO, null, null);
    }

    public static PostingCommand release(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        return new PostingCommand(
            PostingIntent.RELEASE, walletId, amount, currency, movementKey, description,
            LedgerMovementMode.AUTO, null, null);
    }
}
