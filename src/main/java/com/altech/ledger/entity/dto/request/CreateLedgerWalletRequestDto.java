package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;

/** Attach a wallet row to an existing account. */
public record CreateLedgerWalletRequestDto(
    Long accountId,
    String ownerId,
    Currency settlementCurrency,
    String name,
    /** Optional vanity display code. */
    String vanityCode
) {
    public CreateLedgerWalletRequestDto(Long accountId, String ownerId, Currency settlementCurrency, String name) {
        this(accountId, ownerId, settlementCurrency, name, null);
    }
}
