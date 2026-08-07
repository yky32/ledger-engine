package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.enu.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Full ledger account row for list/get (structure + balances + timestamps).
 */
public record GetLedgerAccountResponseDto(
    Long id,
    String fullNumber,
    String entity,
    String type,
    String subType,
    String mainAccount,
    String subAccount,
    String buffer,
    String currency,
    BigDecimal ledgerBalance,
    BigDecimal availableBalance,
    AccountStatus status,
    Instant createDt,
    Instant updateDt
) {}
