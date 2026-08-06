package com.altech.ledger.entity.dto.rule;

import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/** Accounting rule + rule-execution API. */
public final class RuleDtos {
    private RuleDtos() {}

    public record CreateRequest(
        @NotBlank String name,
        String description,
        MovementDirection direction,
        BigDecimal multiplier,
        String targetAccount,
        String content
    ) {}

    public record Response(
        Long id,
        String name,
        String description,
        MovementDirection direction,
        BigDecimal multiplier,
        String targetAccount,
        String content,
        Instant createDt
    ) {}

    public record CreateExecutionRequest(
        @NotBlank String name,
        String description,
        @NotNull OrderType orderType,
        String metadata
    ) {}

    public record ExecutionResponse(
        Long id,
        String name,
        String description,
        OrderType orderType,
        String metadata,
        Instant createDt
    ) {}
}
