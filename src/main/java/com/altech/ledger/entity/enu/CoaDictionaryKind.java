package com.altech.ledger.entity.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

/** One row in the COA dictionary — what a digit segment or stem means. */
public enum CoaDictionaryKind {
    /** 2-digit entity, e.g. 01 = CC. */
    ENTITY,
    /** 2-digit account type, e.g. 02 = Operating. */
    TYPE,
    /** 2-digit sub-type. */
    SUB_TYPE,
    /** entity-type, e.g. 01-02 = house operating. */
    STEM,
    /** entity-type-subType, e.g. 01-02-01. */
    PATH,
    /** 2-digit buffer. */
    BUFFER;

    public static CoaDictionaryKind get(String input) {
        if (input != null) {
            for (CoaDictionaryKind value : CoaDictionaryKind.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(CoaDictionaryKind.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
