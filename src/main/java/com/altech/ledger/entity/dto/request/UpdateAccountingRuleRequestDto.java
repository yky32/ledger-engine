package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.MovementDirection;

import java.math.BigDecimal;

public record UpdateAccountingRuleRequestDto(
    String name,
    String description,
    MovementDirection direction,
    BigDecimal multiplier,
    String targetAccount,
    String content
) {}
