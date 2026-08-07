package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stored FX pair as returned from create/get/list.
 */
public record GetFxRateResponseDto(
    Long id,
    Currency base,
    Currency target,
    BigDecimal rate,
    Instant createDt,
    Instant updateDt
) {}
