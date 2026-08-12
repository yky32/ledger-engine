package com.altech.ledger.entity.dto.ingest;

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
 * Customer key = {@link #ownerId} (same as wallet {@code ownerId}).
 * JSON aliases {@code associatedIdentifier} / {@code userId} accepted for older samples.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionalEvent(
    @NotBlank String eventId,

    /** CRM / customer id — wallet ownerId. */
    @NotBlank
    @JsonAlias({"associatedIdentifier", "userId"})
    String ownerId,

    @NotBlank String eventType,

    @NotNull @PositiveOrZero BigDecimal amount,

    @NotNull Currency currency,

    Instant occurredAt,

    Map<String, String> metadata
) {
    public TransactionalEvent {
        if (ownerId != null) {
            ownerId = ownerId.trim();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
