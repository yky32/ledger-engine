package com.altech.ledger.entity.dto.response;

import java.math.BigDecimal;

/**
 * Balance-only line (optional FX-converted amount for display).
 */
public record GetLedgerAccountBalanceResponseDto(
    Long accountId,
    String currency,
    BigDecimal ledgerBalance,
    BigDecimal availableBalance,
    BigDecimal fxConvertedBalance
) {}
