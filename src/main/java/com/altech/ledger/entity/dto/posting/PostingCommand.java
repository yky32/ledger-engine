package com.altech.ledger.entity.dto.posting;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.PostingIntent;

import java.math.BigDecimal;

/**
 * Central posting command — one shape for money rails + loyalty + hold.
 * Prefer {@link com.altech.ledger.usecase.ledger.ApplyPostingUseCase#execute} over ad-hoc shooter calls.
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
    String externalPartyId,
    /** Unused when an AccountingRuleExecution sequence is present. */
    Long accountId,
    /** Brain / use-case code used to load the posting sequence. */
    String eventType,
    /** Client product book key (event {@code mainAccount}, e.g. 9089… / 9088…). Member legs only. */
    String mainAccount
) {
    public PostingCommand {
        if (mode == null) {
            mode = LedgerMovementMode.AUTO;
        }
        if (mainAccount != null) {
            mainAccount = mainAccount.trim();
            if (mainAccount.isEmpty()) {
                mainAccount = null;
            }
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
            PostingIntent.DEPOSIT, walletId, amount, currency, movementKey, description, mode,
            null, null, null, null, null);
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
            null, externalPartyId, null, null, null);
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
            mode, toWalletId, null, null, null, null);
    }

    public static PostingCommand earn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        return earn(walletId, amount, currency, movementKey, description, null, null);
    }

    public static PostingCommand earn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        Long accountId
    ) {
        return earn(walletId, amount, currency, movementKey, description, accountId, null, null);
    }

    public static PostingCommand earn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        Long accountId,
        String eventType
    ) {
        return earn(walletId, amount, currency, movementKey, description, accountId, eventType, null);
    }

    public static PostingCommand earn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        Long accountId,
        String eventType,
        String mainAccount
    ) {
        return new PostingCommand(
            PostingIntent.EARN, walletId, amount, currency, movementKey, description,
            LedgerMovementMode.AUTO, null, null, accountId, eventType, mainAccount);
    }

    public static PostingCommand burn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        return burn(walletId, amount, currency, movementKey, description, null, null);
    }

    public static PostingCommand burn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        Long accountId
    ) {
        return burn(walletId, amount, currency, movementKey, description, accountId, null, null);
    }

    public static PostingCommand burn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        Long accountId,
        String eventType
    ) {
        return burn(walletId, amount, currency, movementKey, description, accountId, eventType, null);
    }

    public static PostingCommand burn(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        Long accountId,
        String eventType,
        String mainAccount
    ) {
        return new PostingCommand(
            PostingIntent.BURN, walletId, amount, currency, movementKey, description,
            LedgerMovementMode.AUTO, null, null, accountId, eventType, mainAccount);
    }

    public static PostingCommand hold(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        return hold(walletId, amount, currency, movementKey, description, null);
    }

    public static PostingCommand hold(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        Long accountId
    ) {
        return new PostingCommand(
            PostingIntent.HOLD, walletId, amount, currency, movementKey, description,
            LedgerMovementMode.AUTO, null, null, accountId, null, null);
    }

    public static PostingCommand release(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        return release(walletId, amount, currency, movementKey, description, null);
    }

    public static PostingCommand release(
        Long walletId,
        BigDecimal amount,
        Currency currency,
        String movementKey,
        String description,
        Long accountId
    ) {
        return new PostingCommand(
            PostingIntent.RELEASE, walletId, amount, currency, movementKey, description,
            LedgerMovementMode.AUTO, null, null, accountId, null, null);
    }
}
