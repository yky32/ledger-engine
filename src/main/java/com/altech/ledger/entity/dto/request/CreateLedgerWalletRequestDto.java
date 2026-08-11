package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.WalletAssociationType;

/**
 * Attach a wallet row to an existing account (owner, settlement currency, external ids).
 */
public record CreateLedgerWalletRequestDto(
    Long accountId,
    String associatedIdentifier,
    String associatedFrom,
    WalletAssociationType type,
    String ownerId,
    /** Default settlement currency. */
    Currency settlementCurrency,
    String nickname
) {
    public CreateLedgerWalletRequestDto {
    }
}
