package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.enu.OrderType;

import java.time.Instant;

public record GetAccountingRuleExecutionResponseDto(
    Long id,
    String name,
    String description,
    OrderType orderType,
    String eventType,
    String metadata,
    Instant createDt
) {}
