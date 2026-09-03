package com.altech.ledger.entity.dto.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.IngestAction;
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
    String mainAccount,

    /**
     * Refund pointer — earn/burn {@code eventId} to reverse.
     * Prefer this field; metadata.originalEventId still accepted.
     */
    @JsonAlias({"original_event_id", "originalTxnId", "original_txn_id"})
    String originalEventId,

    /**
     * {@link IngestAction#SPEND} (default) · {@code REFUND} · {@code VOID} · {@code PARTIAL}
     * · {@code CHARGEBACK} · {@code ADJUST}. JSON {@code ORIGINAL}/{@code APPLY}/{@code NORMAL} → SPEND.
     */
    @JsonAlias({"booking", "intent"})
    IngestAction action
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
        if (originalEventId != null) {
            originalEventId = originalEventId.trim();
            if (originalEventId.isEmpty()) {
                originalEventId = null;
            }
        }
        if (metadata == null) {
            metadata = Map.of();
        } else {
            if (mainAccount == null) {
                String fromMeta = metadata.get("mainAccount");
                if (fromMeta == null) {
                    fromMeta = metadata.get("main_account");
                }
                if (fromMeta != null && !fromMeta.isBlank()) {
                    mainAccount = fromMeta.trim();
                }
            }
            if (originalEventId == null) {
                for (String k : new String[] {
                    "originalEventId", "original_event_id", "originalTxnId", "original_txn_id"
                }) {
                    String v = metadata.get(k);
                    if (v != null && !v.isBlank()) {
                        originalEventId = v.trim();
                        break;
                    }
                }
            }
        }
        if (action == null) {
            action = inferAction(eventType, originalEventId);
        }
    }

    public boolean isRefund() {
        return action != null && action.isFullReverse();
    }

    private static IngestAction inferAction(String eventType, String originalEventId) {
        String t = eventType == null ? "" : eventType.trim().toUpperCase();
        if (t.endsWith("_VOID") || t.endsWith("_REVERSAL")) {
            return IngestAction.VOID;
        }
        if (t.endsWith("_CHARGEBACK")) {
            return IngestAction.CHARGEBACK;
        }
        if (t.endsWith("_PARTIAL")) {
            return IngestAction.PARTIAL;
        }
        if (t.endsWith("_ADJUST")) {
            return IngestAction.ADJUST;
        }
        if (t.endsWith("_REFUND") || "REFUND".equals(t) || originalEventId != null) {
            return IngestAction.REFUND;
        }
        return IngestAction.SPEND;
    }

    /** Tests / replay — no client mainAccount / originalEventId / action. */
    public static TransactionalEvent of(
        String eventId,
        String ownerId,
        String eventType,
        BigDecimal amount,
        Currency currency,
        Instant occurredAt,
        Map<String, String> metadata
    ) {
        return new TransactionalEvent(
            eventId, ownerId, eventType, amount, currency, occurredAt, metadata, null, null, null);
    }
}
