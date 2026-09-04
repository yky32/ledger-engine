package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

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

    public static MovementDirection get(String input) {
        if (input != null) {
            for (MovementDirection value : MovementDirection.values()) {
                if (input.equalsIgnoreCase(value.name()) || input.equalsIgnoreCase(value.getShortForm())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(MovementDirection.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
