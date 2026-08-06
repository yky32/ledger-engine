package com.altech.ledger.entity.dto.wallet;

import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.BalanceResponse;
import com.altech.ledger.entity.enu.WalletStatus;

public record WalletOnboardResponse(
    Long walletId,
    String alias,
    String ownerId,
    String currency,
    WalletStatus status,
    String externalId,
    String externalType,
    AccountResponse account,
    BalanceResponse balance
) {}
