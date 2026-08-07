package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;

/**
 * Partial update of wallet status / account / external identity / nickname.
 */
public record UpdateLedgerWalletRequestDto(
    WalletStatus status,
    Long accountId,
    String extIdentifier,
    WalletAssociationType type,
    String nickname
) {}
