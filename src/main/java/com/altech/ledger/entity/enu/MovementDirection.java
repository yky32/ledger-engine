package com.altech.ledger.entity.enu;

/** Port of payment-gateway {@code MovementDirection}. */
public enum MovementDirection {
    DEBIT("DR"),
    CREDIT("CR");

    private final String shortForm;

    MovementDirection(String shortForm) {
        this.shortForm = shortForm;
    }

    public String getShortForm() {
        return shortForm;
    }
}
