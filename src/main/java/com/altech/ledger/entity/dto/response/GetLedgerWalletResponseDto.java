package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.WalletStatus;

import java.time.Instant;
import java.util.List;

/**
 * Lean wallet + related account balances.
 * {@code associatedIdentifier} mirrors {@code ownerId} (API compatibility).
 */
public record GetLedgerWalletResponseDto(
    Long id,
    Long accountId,
    String ownerId,
    /** Same as ownerId — CRM / customer id. */
    String associatedIdentifier,
    String name,
    WalletStatus status,
    Currency settlementCurrency,
    List<GetLedgerAccountResponseDto> accounts,
    Instant createDt,
    Instant updateDt
) {}
