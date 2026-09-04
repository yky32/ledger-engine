package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

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
    ADJUST,
    ;

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

    /** Blank / omit → SPEND. Unknown token → {@code PAM0400}. */
    @JsonCreator
    public static IngestAction get(String input) {
        if (input == null || input.isBlank()) {
            return SPEND;
        }
        String t = input.trim();
        if ("REVERSE".equalsIgnoreCase(t)) {
            return VOID;
        }
        if ("DISPUTE".equalsIgnoreCase(t)) {
            return CHARGEBACK;
        }
        if ("ADJUSTMENT".equalsIgnoreCase(t)) {
            return ADJUST;
        }
        if ("ORIGINAL".equalsIgnoreCase(t) || "APPLY".equalsIgnoreCase(t) || "NORMAL".equalsIgnoreCase(t)) {
            return SPEND;
        }
        for (IngestAction value : IngestAction.values()) {
            if (t.equalsIgnoreCase(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s",
            input, input, Arrays.asList(IngestAction.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
