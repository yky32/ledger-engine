package com.altech.core.common;

public class AppContextHolder {
    public static final ThreadLocal<AppContext> CONTEXT = new ThreadLocal<>();

    public static void clearContext() {
        CONTEXT.remove();
    }
}
