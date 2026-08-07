package com.altech.ledger.entity.dto.parity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;

/** FX rate setup and query API. */
public final class FxRateDtos {
    private FxRateDtos() {}

    public record CreateRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String base,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String target,
        @NotNull BigDecimal rate
    ) {
        public CreateRequest {
            if (base != null) {
                base = base.trim().toUpperCase();
            }
            if (target != null) {
                target = target.trim().toUpperCase();
            }
        }
    }

    public record Response(
        Long id,
        String base,
        String target,
        BigDecimal rate,
        Instant createDt,
        Instant updateDt
    ) {}
}
