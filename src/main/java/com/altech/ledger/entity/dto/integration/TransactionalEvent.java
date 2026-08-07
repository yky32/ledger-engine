package com.altech.ledger.entity.dto.integration;

import com.altech.core.constant.enu.Currency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Inbound loyalty / commerce event for rule evaluation (webhook or Kafka ingest).
 * <p>
 * Matched by {@code eventType} against integration rules (earn/burn/process).
 */
public record TransactionalEvent(
    @NotBlank String eventId,
    @NotBlank String userId,
    @NotBlank String eventType,
    @NotNull @PositiveOrZero BigDecimal amount,
    @NotNull Currency currency,
    Instant occurredAt,
    Map<String, String> metadata
) {}
