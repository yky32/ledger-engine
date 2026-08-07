package com.altech.core.utils;

import java.util.concurrent.Callable;

public class CallableUtil {
    public static <T> T call(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw ExceptionUtil.getRuntimeException(e);
        }
    }
}
