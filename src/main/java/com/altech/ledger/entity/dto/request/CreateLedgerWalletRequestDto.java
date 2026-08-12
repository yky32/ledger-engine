package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;

/** Attach a wallet row to an existing account. */
public record CreateLedgerWalletRequestDto(
    Long accountId,
    String ownerId,
    Currency settlementCurrency,
    String name
) {}
