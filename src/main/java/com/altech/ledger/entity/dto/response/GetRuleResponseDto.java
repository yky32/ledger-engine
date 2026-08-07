package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.enu.MovementDirection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Rule catalog entry response.
 */
public record GetRuleResponseDto(
    Long id,
    String name,
    String description,
    MovementDirection direction,
    BigDecimal multiplier,
    String targetAccount,
    String content,
    Instant createDt
) {}
