package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;

import java.time.Instant;
import java.util.List;

/** Lean wallet + account balances. Query key = {@code ownerId}. */
public record GetLedgerWalletResponseDto(
    Long id,
    Long accountId,
    String ownerId,
    String vanityCode,
    String name,
    WalletAssociationType type,
    WalletType walletType,
    WalletStatus status,
    Currency settlementCurrency,
    String tier,
    List<GetLedgerAccountResponseDto> accounts,
    Instant createDt,
    Instant updateDt
) {}
