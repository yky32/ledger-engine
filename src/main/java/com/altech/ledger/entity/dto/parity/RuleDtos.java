package com.altech.ledger.entity.dto.parity;

import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Accounting rule + rule-execution API ({@code /rules}, {@code /rule-executions}).
 */
public final class RuleDtos {
    private RuleDtos() {}

    /**
     * Create a catalog rule (direction, multiplier, target account, content).
     */
    public record CreateRequest(
        @NotBlank String name,
        String description,
        MovementDirection direction,
        BigDecimal multiplier,
        String targetAccount,
        String content
    ) {}

    /**
     * Rule catalog entry response.
     */
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

    /**
     * Bind an {@link OrderType} to execution metadata (often rule id list JSON).
     */
    public record CreateExecutionRequest(
        @NotBlank String name,
        String description,
        @NotNull OrderType orderType,
        String metadata
    ) {}

    /**
     * Rule-execution config response (looked up by order type at settle time).
     */
    public record ExecutionResponse(
        Long id,
        String name,
        String description,
        OrderType orderType,
        String metadata,
        Instant createDt
    ) {}
}
