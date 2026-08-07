package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;

import com.altech.ledger.entity.enu.WalletAssociationType;

/**
 * Attach a wallet row to an existing account (owner, currency, external ids).
 */
public record CreateLedgerWalletRequestDto(
    Long accountId,
    String extIdentifier,
    String extType,
    WalletAssociationType type,
    String ownerId,
    Currency currency,
    String nickname
) {
    public CreateLedgerWalletRequestDto {
    }
}
