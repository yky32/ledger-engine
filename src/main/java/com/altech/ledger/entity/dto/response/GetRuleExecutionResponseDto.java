package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.enu.OrderType;

import java.time.Instant;

public record GetRuleExecutionResponseDto(
    Long id,
    String name,
    String description,
    OrderType orderType,
    Object metadata,
    Instant createDt
) {}
