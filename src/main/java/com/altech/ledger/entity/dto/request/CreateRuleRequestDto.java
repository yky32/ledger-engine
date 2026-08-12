package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.MovementDirection;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateRuleRequestDto(
    @NotBlank String name,
    String description,
    MovementDirection direction,
    BigDecimal multiplier,
    String targetAccount,
    Object content
) {}
