package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;

import java.time.Instant;
import java.util.List;

/**
 * Wallet plus related multi-currency account balances under the main account.
 */
public record GetLedgerWalletResponseDto(
    Long id,
    String alias,
    Long accountId,
    String nickname,
    String extIdentifier,
    String extType,
    WalletAssociationType type,
    WalletType walletType,
    WalletStatus status,
    String ownerId,
    String currency,
    List<GetLedgerAccountResponseDto> accounts,
    Instant createDt,
    Instant updateDt
) {}
