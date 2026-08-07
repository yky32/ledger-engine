package com.altech.ledger.entity.enu;

/** AccountStatus. */
public enum AccountStatus {
    NEW(false),
    VERIFIED(false),
    ACTIVE(true),
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
