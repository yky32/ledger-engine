package com.altech.core.constant.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

public enum CurrencyType {
    FIAT,
    CRYPTO,
    LOYALTY_POINT,
    ALL
    ;

    public static CurrencyType get(String input) {
        for (CurrencyType value : CurrencyType.values()) {
            if (input.equalsIgnoreCase(value.name())) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(CurrencyType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
