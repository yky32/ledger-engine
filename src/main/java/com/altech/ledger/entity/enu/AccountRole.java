package com.altech.ledger.entity.enu;

/**
 * Product role of an account inside an {@link com.altech.ledger.entity.po.ledger.AccountSet}.
 * Phase A structure only — Phase B journal applies balance rules by role.
 */
public enum AccountRole {
    /** Spendable / earn target book for a currency. */
    AVAILABLE,
    /** Held / pending (not spendable). */
    HELD,
    /** Cumulative redeemed / burned (LP). */
    REDEEMED,
    /** Cumulative expired (LP). */
    EXPIRED,
    /** Manual adjustments. */
    ADJUST
}
