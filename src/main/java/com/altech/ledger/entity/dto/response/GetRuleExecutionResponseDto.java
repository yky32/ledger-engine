package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.enu.OrderType;

import java.time.Instant;

/**
 * Rule-execution config response (looked up by order type at settle time).
 */
public record GetRuleExecutionResponseDto(
    Long id,
    String name,
    String description,
    OrderType orderType,
    String metadata,
    Instant createDt
) {}
