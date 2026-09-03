package com.altech.core.aop.log;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

public enum LogScope {
    ENDPOINT,
    SERVICE,
    API,
    REPOSITORY,
    METHOD,
    ;

    public static LogScope get(String input) {
        if (input != null) {
            for (LogScope value : LogScope.values()) {
                if (input.equalsIgnoreCase(value.name())) {
                    return value;
                }
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(LogScope.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
