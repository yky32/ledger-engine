package com.altech.ledger.entity.dto.wallet;

import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.BalanceResponse;
import com.altech.ledger.entity.po.Wallet;

import java.util.UUID;

public record WalletOnboardResponse(
    UUID walletId,
    String alias,
    String ownerId,
    String currency,
    Wallet.Status status,
    String externalId,
    String externalType,
    AccountResponse account,
    BalanceResponse balance
) {}
