package com.altech.ledger.entity.enu;

/** WalletStatus. */
public enum WalletStatus {
    PENDING(false),
    VERIFIED(false),
    ACTIVE(true),
    DORMANT(true),
    CLOSED(true),
    SUSPENDED(true);

    private final boolean isFinal;

    WalletStatus(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isFinal() {
        return isFinal;
    }
}
