package com.altech.core.api;

import java.util.List;

/**
 * Dynamically for embedded different handlers implementation
 *
 * @param <T> - TenantContext Stored in ThreadLocal
 * @param <P> - Query Parameter
 */
public interface ApiClient<T, P> {
    default T execute(P p) {
        return null;
    }

    default List<T> execute(List<String> ps) {
        return null;
    }

    default List<Long> getIds(List<String> ps) {
        return null;
    }

    default void executeOnly(P p) {
    }
}
