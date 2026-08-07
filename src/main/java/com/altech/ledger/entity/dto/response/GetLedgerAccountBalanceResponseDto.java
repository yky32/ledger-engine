package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;

import java.math.BigDecimal;

/**
 * Balance-only line (optional FX-converted amount for display).
 */
public record GetLedgerAccountBalanceResponseDto(
    Long accountId,
    Currency currency,
    BigDecimal ledgerBalance,
    BigDecimal availableBalance,
    BigDecimal fxConvertedBalance
) {}
