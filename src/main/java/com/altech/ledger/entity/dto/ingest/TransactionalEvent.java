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

    Map<String, String> metadata,

    /**
     * Optional client main-account key (e.g. UAF {@code 9089…} / {@code 9088…}).
     * When blank, engine allocates {@code account.main_account}.
     */
    @JsonAlias({"main_account"})
    String mainAccount
) {
    public TransactionalEvent {
        if (ownerId != null) {
            ownerId = ownerId.trim();
        }
        if (mainAccount != null) {
            mainAccount = mainAccount.trim();
            if (mainAccount.isEmpty()) {
                mainAccount = null;
            }
        }
        if (metadata == null) {
            metadata = Map.of();
        } else if (mainAccount == null) {
            String fromMeta = metadata.get("mainAccount");
            if (fromMeta == null) {
                fromMeta = metadata.get("main_account");
            }
            if (fromMeta != null && !fromMeta.isBlank()) {
                mainAccount = fromMeta.trim();
            }
        }
    }

    /** Compact ctor used by tests — no client mainAccount. */
    public TransactionalEvent(
        String eventId,
        String ownerId,
        String eventType,
        BigDecimal amount,
        Currency currency,
        Instant occurredAt,
        Map<String, String> metadata
    ) {
        this(eventId, ownerId, eventType, amount, currency, occurredAt, metadata, null);
    }
}
