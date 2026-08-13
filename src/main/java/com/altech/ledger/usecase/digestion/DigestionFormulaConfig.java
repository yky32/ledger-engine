package com.altech.ledger.usecase.digestion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Dynamic <b>JSON</b> formula config for digestion scoring (Brain).
 * <p>
 * Canonical shape stored as JSONB on {@code DigestionRule.formula}:
 * <pre>
 * { "type": "AMOUNT" }
 * { "type": "RATE",   "rate": 0.01 }
 * { "type": "FIXED",  "value": 100 }
 * { "type": "LINEAR", "rate": 0.01, "fixed": 5 }   // amount * rate + fixed
 * </pre>
 * Legacy string DSL ({@code RATE:0.01}, {@code FIXED:100}, {@code MUL_ADD:…}) is still
 * accepted on write and normalized to JSON.
 */
public final class DigestionFormulaConfig {

    public static final String TYPE_AMOUNT = "AMOUNT";
    public static final String TYPE_RATE = "RATE";
    public static final String TYPE_FIXED = "FIXED";
    public static final String TYPE_LINEAR = "LINEAR";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private DigestionFormulaConfig() {}

    public static Map<String, Object> ofAmount() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", TYPE_AMOUNT);
        return m;
    }

    public static Map<String, Object> ofRate(BigDecimal rate) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", TYPE_RATE);
        m.put("rate", rate);
        return m;
    }

    public static Map<String, Object> ofFixed(BigDecimal value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", TYPE_FIXED);
        m.put("value", value);
        return m;
    }

    public static Map<String, Object> ofLinear(BigDecimal rate, BigDecimal fixed) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", TYPE_LINEAR);
        m.put("rate", rate != null ? rate : BigDecimal.ZERO);
        m.put("fixed", fixed != null ? fixed : BigDecimal.ZERO);
        return m;
    }

    /**
     * Normalize API / DB value → canonical {@link LinkedHashMap}. Throws if unsupported.
     */
    public static Map<String, Object> normalize(Object raw) {
        if (raw == null) {
            return ofAmount();
        }
        if (raw instanceof String s) {
            return fromLegacyString(s.trim());
        }

        Map<String, Object> in = coerceToStringKeyMap(raw);
        if (in.isEmpty()) {
            // last resort Jackson
            try {
                Map<String, Object> via = MAPPER.convertValue(raw, MAP_TYPE);
                if (via != null) {
                    in = via;
                }
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        if (in == null || in.isEmpty()) {
            throw new IllegalArgumentException(
                "formula must be a non-empty JSON object, got " + raw.getClass().getName() + ": " + raw);
        }

        String type = str(in.get("type"));
        if (type == null || type.isBlank()) {
            if (in.containsKey("rate") || in.containsKey("fixed")) {
                return ofLinear(decimal(in.get("rate")), decimal(in.get("fixed")));
            }
            throw new IllegalArgumentException(
                "formula.type is required; keys=" + in.keySet() + " raw=" + raw);
        }
        type = type.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case TYPE_AMOUNT, "AMT" -> ofAmount();
            case TYPE_RATE, "PERCENT", "PCT" -> {
                BigDecimal rate = decimal(in.get("rate"));
                if (rate == null) {
                    throw new IllegalArgumentException("formula.rate required for type RATE");
                }
                yield ofRate(rate);
            }
            case TYPE_FIXED, "CONST", "CONSTANT" -> {
                BigDecimal v = decimal(in.get("value"));
                if (v == null) {
                    v = decimal(in.get("fixed"));
                }
                if (v == null) {
                    throw new IllegalArgumentException("formula.value required for type FIXED");
                }
                yield ofFixed(v);
            }
            case TYPE_LINEAR, "MUL_ADD", "RATE_FIXED" ->
                ofLinear(decimal(in.get("rate")), decimal(first(in, "fixed", "value")));
            default -> throw new IllegalArgumentException("Unsupported formula.type: " + type);
        };
    }

    /** Copy any Map-like (Java / Jackson / wrapper) into a mutable LinkedHashMap. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Object> coerceToStringKeyMap(Object raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (raw instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            return out;
        }
        // Iterable of entries?
        try {
            Object entrySet = raw.getClass().getMethod("entrySet").invoke(raw);
            if (entrySet instanceof Iterable<?> it) {
                for (Object o : it) {
                    if (o instanceof Map.Entry<?, ?> e && e.getKey() != null) {
                        out.put(String.valueOf(e.getKey()), e.getValue());
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // ignore
        }
        return out;
    }

    public static BigDecimal compute(Object formula, BigDecimal amount) {
        Map<String, Object> cfg = normalize(formula);
        String type = String.valueOf(cfg.get("type"));
        BigDecimal amt = amount == null ? BigDecimal.ZERO : amount;
        return switch (type) {
            case TYPE_AMOUNT -> scale(amt);
            case TYPE_RATE -> scale(amt.multiply(require(decimal(cfg.get("rate")), "rate")));
            case TYPE_FIXED -> scale(require(decimal(cfg.get("value")), "value"));
            case TYPE_LINEAR -> {
                BigDecimal rate = decimal(cfg.get("rate"));
                BigDecimal fixed = decimal(cfg.get("fixed"));
                if (rate == null) {
                    rate = BigDecimal.ZERO;
                }
                if (fixed == null) {
                    fixed = BigDecimal.ZERO;
                }
                yield scale(amt.multiply(rate).add(fixed));
            }
            default -> throw new IllegalArgumentException("Unsupported formula.type: " + type);
        };
    }

    public static boolean isSpendBased(Object formula) {
        try {
            Map<String, Object> cfg = normalize(formula);
            return !TYPE_FIXED.equals(String.valueOf(cfg.get("type")));
        } catch (RuntimeException ex) {
            return true;
        }
    }

    /** Parse legacy string DSL into JSON config. */
    public static Map<String, Object> fromLegacyString(String formula) {
        if (formula == null || formula.isBlank()) {
            return ofAmount();
        }
        String raw = formula.trim();
        if (raw.startsWith("{")) {
            try {
                Map<String, Object> m = MAPPER.readValue(raw, MAP_TYPE);
                return normalize(m);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid formula JSON string: " + formula, ex);
            }
        }
        String f = raw.toUpperCase(Locale.ROOT);
        if ("AMOUNT".equals(f) || "AMT".equals(f)) {
            return ofAmount();
        }
        if (f.startsWith("FIXED:")) {
            return ofFixed(new BigDecimal(raw.substring("FIXED:".length()).trim()));
        }
        if (f.startsWith("RATE:")) {
            return ofRate(new BigDecimal(raw.substring("RATE:".length()).trim()));
        }
        if (f.startsWith("MUL_ADD:")) {
            String rest = raw.substring("MUL_ADD:".length()).trim();
            String[] parts = rest.split(":");
            BigDecimal rate = new BigDecimal(parts[0].trim());
            BigDecimal fixed = parts.length > 1 ? new BigDecimal(parts[1].trim()) : BigDecimal.ZERO;
            return ofLinear(rate, fixed);
        }
        throw new IllegalArgumentException("Unsupported legacy formula: " + formula);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(18, RoundingMode.HALF_UP);
    }

    private static BigDecimal require(BigDecimal v, String name) {
        if (v == null) {
            throw new IllegalArgumentException("formula." + name + " is required");
        }
        return v;
    }

    private static BigDecimal decimal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        if (o instanceof Integer i) {
            return BigDecimal.valueOf(i.longValue());
        }
        if (o instanceof Long l) {
            return BigDecimal.valueOf(l);
        }
        if (o instanceof Number n) {
            // Double/Float from JSON — use valueOf carefully
            return new BigDecimal(n.toString());
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return new BigDecimal(s);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Object first(Map<String, Object> m, String a, String b) {
        if (m.containsKey(a) && m.get(a) != null) {
            return m.get(a);
        }
        return m.get(b);
    }
}
