package com.altech.ledger.entity.enu;

/**
 * Upstream webhook / Brain / accounting {@code eventType} tokens.
 * Reward (loyalty LP vs cashback HKD) is Brain {@code resultCurrency}, not a suffix here.
 */
public final class EventTypes {
    /** Credit card transaction. */
    public static final String CC_TXN = "CC_TXN";
    /**
     * Legacy combined token. Prefer {@code eventType=CC_TXN} + {@code action=REFUND}
     * + {@code originalEventId}. Still recognised as refund.
     */
    public static final String CC_TXN_REFUND = "CC_TXN_REFUND";
    /** Credit card cash instalment. */
    public static final String CC_CIP = "CC_CIP";
    /** Credit card spending instalment. */
    public static final String CC_SIP = "CC_SIP";
    /** Loan transaction. */
    public static final String LN_TXN = "LN_TXN";

    private EventTypes() {
    }
}
