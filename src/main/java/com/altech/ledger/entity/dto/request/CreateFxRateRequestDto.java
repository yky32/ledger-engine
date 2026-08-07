package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Create or update FX pair: base → target rate (codes uppercased).
 */
public record CreateFxRateRequestDto(
    @NotNull Currency base,
    @NotNull Currency target,
    @NotNull BigDecimal rate
) {
    public CreateFxRateRequestDto {
    }
}
