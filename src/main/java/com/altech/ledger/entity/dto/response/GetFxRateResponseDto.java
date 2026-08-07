package com.altech.ledger.entity.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stored FX pair as returned from create/get/list.
 */
public record GetFxRateResponseDto(
    Long id,
    String base,
    String target,
    BigDecimal rate,
    Instant createDt,
    Instant updateDt
) {}
