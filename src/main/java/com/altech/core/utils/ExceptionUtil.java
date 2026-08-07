package com.altech.core.utils;

public class ExceptionUtil {
    public static RuntimeException getRuntimeException(Exception e) {
        if(e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }
}
