package com.altech.ledger.entity.dto.request;

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
    String currency,
    String nickname
) {
    public CreateLedgerWalletRequestDto {
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
    }
}
