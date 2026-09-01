package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.MovementDirection;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateAccountingRuleRequestDto(
    @NotBlank String name,
    String description,
    MovementDirection direction,
    BigDecimal multiplier,
    /** CoaProfile.code — chart structure, not a member account id. */
    String targetAccount,
    String content
) {}
