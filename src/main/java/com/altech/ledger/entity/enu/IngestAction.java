package com.altech.ledger.entity.enu;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How to book an inbound event. Separate from {@code eventType} (CC_TXN / CC_CIP / …).
 * Omit on the first fire — default {@link #SPEND}.
 */
public enum IngestAction {
    /** First booking (Door → Brain → books). Default. */
    SPEND,
    /** Customer refund — reverse original books. */
    REFUND,
    /** Same-day cancel / never captured — same reverse as REFUND, different label. */
    VOID,
    /** Refund part of the original earn. Not booked yet (needs this event's amount). */
    PARTIAL,
    /** Issuer dispute — full reverse for now (fee later). */
    CHARGEBACK,
    /** Tip / amount correction on the original. Not booked yet. */
    ADJUST;

    @JsonValue
    public String json() {
        return name();
    }

    /** Full DR/CR reverse of {@code originalEventId} (no Brain). */
    public boolean isFullReverse() {
        return this == REFUND || this == VOID || this == CHARGEBACK;
    }

    /** Known action, booking not implemented. */
    public boolean isUnsupported() {
        return this == PARTIAL || this == ADJUST;
    }

    @JsonCreator
    public static IngestAction from(String raw) {
        if (raw == null || raw.isBlank()) {
            return SPEND;
        }
        return switch (raw.trim().toUpperCase()) {
            case "REFUND" -> REFUND;
            case "VOID", "REVERSE" -> VOID;
            case "PARTIAL" -> PARTIAL;
            case "CHARGEBACK", "DISPUTE" -> CHARGEBACK;
            case "ADJUST", "ADJUSTMENT" -> ADJUST;
            case "SPEND", "ORIGINAL", "APPLY", "NORMAL" -> SPEND;
            default -> SPEND;
        };
    }
}
