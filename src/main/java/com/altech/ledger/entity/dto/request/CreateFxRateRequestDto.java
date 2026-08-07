package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Create or update FX pair: base → target rate (codes uppercased).
 */
public record CreateFxRateRequestDto(
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String base,
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String target,
    @NotNull BigDecimal rate
) {
    public CreateFxRateRequestDto {
        if (base != null) {
            base = base.trim().toUpperCase();
        }
        if (target != null) {
            target = target.trim().toUpperCase();
        }
    }
}
