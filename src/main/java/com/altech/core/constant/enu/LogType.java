package com.altech.core.constant.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;

import java.util.Arrays;

public enum LogType {
    ACTIVITY,
    AUDIT_TRAIL,
    ;

    public static LogType get(String input) {
        for (LogType value : LogType.values()) {
            if (input.equalsIgnoreCase(value.name())){
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(LogType.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
