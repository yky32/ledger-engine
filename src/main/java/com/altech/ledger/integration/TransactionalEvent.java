package com.altech.ledger.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record TransactionalEvent(
    @NotBlank String eventId,
    @NotBlank String userId,
    @NotBlank String eventType,
    @NotNull @PositiveOrZero BigDecimal amount,
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
    Instant occurredAt,
    Map<String, String> metadata
) {}
