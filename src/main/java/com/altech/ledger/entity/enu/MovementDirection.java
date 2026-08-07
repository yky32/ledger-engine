package com.altech.ledger.entity.enu;

/** MovementDirection. */
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
