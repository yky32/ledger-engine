package com.altech.ledger.usecase.factor;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared factor predicate: {@code field + op + value}.
 * <p>
 * Used by Door {@code entryFactors} (is entered?) and Brain {@code whenFactors} (which equation).
 * <pre>
 * { "field": "currency", "op": "in", "value": ["HKD","USD"] }
 * { "field": "amount", "op": "between", "value": { "min": 1, "max": 10000 } }
 * { "field": "ageDays", "op": "lte", "value": 30 }
 * { "field": "mcc", "op": "nin", "value": ["6010"] }
 * { "field": "metadata.channel", "op": "eq", "value": "POS" }
 * </pre>
 */
public final class FactorSpec {

    private FactorSpec() {}

    public static String fieldOf(Map<String, Object> f) {
        if (f == null) {
            return null;
        }
        Object v = first(f, "field", "name");
        return v == null ? null : String.valueOf(v).trim();
    }

    public static String opOf(Map<String, Object> f) {
        if (f == null) {
            return "eq";
        }
        Object v = first(f, "op", "operator");
        if (v == null || String.valueOf(v).isBlank()) {
            return "eq";
        }
        return String.valueOf(v).trim().toLowerCase(Locale.ROOT);
    }

    public static Object valueOf(Map<String, Object> f) {
        if (f == null) {
            return null;
        }
        if (f.containsKey("value")) {
            return f.get("value");
        }
        return f.get("values");
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> asFactorList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream()
                .filter(Map.class::isInstance)
                .map(m -> (Map<String, Object>) m)
                .toList();
        }
        return List.of();
    }

    private static Object first(Map<String, Object> m, String a, String b) {
        if (m.containsKey(a) && m.get(a) != null) {
            return m.get(a);
        }
        return m.get(b);
    }
}
