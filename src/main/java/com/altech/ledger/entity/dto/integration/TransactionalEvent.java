package com.altech.ledger.entity.dto.integration;

import com.altech.core.constant.enu.Currency;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Inbound commerce / loyalty event (webhook or Kafka).
 * <p>
 * Customer key is {@link #associatedIdentifier} — same value used at wallet onboard.
 * JSON alias {@code userId} is accepted for older samples only.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionalEvent(
    @NotBlank String eventId,

    /** CUST_ID / CRM id — must match wallet onboard {@code associatedIdentifier}. */
    @NotBlank
    @JsonAlias("userId")
    String associatedIdentifier,

    @NotBlank String eventType,

    @NotNull @PositiveOrZero BigDecimal amount,

    @NotNull Currency currency,

    /** Event time; required when rule has {@code maxAgeDays}. */
    Instant occurredAt,

    Map<String, String> metadata
) {
    public TransactionalEvent {
        if (associatedIdentifier != null) {
            associatedIdentifier = associatedIdentifier.trim();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
