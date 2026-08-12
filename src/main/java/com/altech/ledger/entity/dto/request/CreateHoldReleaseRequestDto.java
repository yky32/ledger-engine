package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** HOLD or RELEASE available balance (ledger unchanged). */
public record CreateHoldReleaseRequestDto(
    @NotBlank String associatedIdentifier,
    @NotNull Currency currency,
    @NotNull @Positive BigDecimal amount,
    String movementKey,
    String description
) {}
