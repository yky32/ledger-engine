package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;

/**
 * Attach a wallet row to an existing account.
 */
public record CreateLedgerWalletRequestDto(
    Long accountId,
    /** CRM id; used as ownerId when ownerId blank. */
    String associatedIdentifier,
    String ownerId,
    Currency settlementCurrency,
    String name
) {}
