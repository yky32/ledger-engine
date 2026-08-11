package com.altech.ledger.entity.enu;

/** Account lifecycle status (spec: Active, Frozen, Closed + legacy). */
public enum AccountStatus {
    NEW(false),
    VERIFIED(false),
    ACTIVE(true),
    /** Spec frozen — postings blocked in later phases. */
    FROZEN(true),
    DORMANT(true),
    CLOSED(true),
    SUSPENDED(true);

    private final boolean isFinal;

    AccountStatus(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isFinal() {
        return isFinal;
    }
}
