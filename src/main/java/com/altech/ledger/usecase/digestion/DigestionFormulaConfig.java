package com.altech.ledger.usecase.digestion;

import com.altech.core.utils.JSONUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Uses {@link JSONUtil} (tgt-style) for Map/string coercion — no private ObjectMapper.
 * Legacy string DSL is still accepted on write and normalized to JSON.
 */
public final class DigestionFormulaConfig {

    public static final String TYPE_AMOUNT = "AMOUNT";
    public static final String TYPE_RATE = "RATE";
    public static final String TYPE_FIXED = "FIXED";
    public static final String TYPE_LINEAR = "LINEAR";
    public static final String TYPE_TIERED_RATE = "TIERED_RATE";
    public static final String TYPE_TABLE = "TABLE";

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
            String t = s.trim();
            if (t.startsWith("{")) {
                return normalize(JSONUtil.toMap(t));
            }
            return fromLegacyString(t);
        }

        Map<String, Object> in;
        try {
            in = JSONUtil.toMap(raw);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                "formula must be a JSON object, got " + raw.getClass().getName() + ": " + raw, ex);
        }
        if (in.isEmpty()) {
            throw new IllegalArgumentException(
                "formula must be a non-empty JSON object, got " + raw.getClass().getName() + ": " + raw);
        }

        String type = str(in.get("type"));
        if (type == null || type.isBlank()) {
            if (in.containsKey("rate") || in.containsKey("fixed")) {
                return ofLinear(decimal(in.get("rate")), decimal(in.get("fixed")));
            }
            throw new IllegalArgumentException(
                "formula.type is required; keys=" + in.keySet() + " raw=" + JSONUtil.toJson(raw));
        }
        type = type.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case TYPE_AMOUNT, "AMT" -> {
                Map<String, Object> m = ofAmount();
                copyExtras(in, m);
                yield m;
            }
            case TYPE_RATE, "PERCENT", "PCT" -> {
                BigDecimal rate = decimal(in.get("rate"));
                if (rate == null) {
                    throw new IllegalArgumentException("formula.rate required for type RATE");
                }
                Map<String, Object> m = ofRate(rate);
                copyExtras(in, m);
                yield m;
            }
            case TYPE_FIXED, "CONST", "CONSTANT" -> {
                BigDecimal v = decimal(in.get("value"));
                if (v == null) {
                    v = decimal(in.get("fixed"));
                }
                if (v == null) {
                    throw new IllegalArgumentException("formula.value required for type FIXED");
                }
                Map<String, Object> m = ofFixed(v);
                copyExtras(in, m);
                yield m;
            }
            case TYPE_LINEAR, "MUL_ADD", "RATE_FIXED" -> {
                Map<String, Object> m = ofLinear(decimal(in.get("rate")), decimal(first(in, "fixed", "value")));
                copyExtras(in, m);
                yield m;
            }
            case TYPE_TIERED_RATE, "TIERED", "BRACKETS" -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", TYPE_TIERED_RATE);
                Object brackets = in.get("brackets");
                if (brackets == null) {
                    brackets = in.get("tiers");
                }
                if (!(brackets instanceof List<?>) || ((List<?>) brackets).isEmpty()) {
                    throw new IllegalArgumentException("formula.brackets required for TIERED_RATE");
                }
                m.put("brackets", brackets);
                copyExtras(in, m);
                yield m;
            }
            case TYPE_TABLE, "LOOKUP" -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", TYPE_TABLE);
                Object by = first(in, "by", "key");
                if (by == null || String.valueOf(by).isBlank()) {
                    throw new IllegalArgumentException("formula.by required for TABLE");
                }
                m.put("by", String.valueOf(by).trim());
                Object map = in.get("map");
                if (map == null) {
                    map = in.get("cases");
                }
                if (!(map instanceof Map<?, ?>) || ((Map<?, ?>) map).isEmpty()) {
                    throw new IllegalArgumentException("formula.map required for TABLE");
                }
                m.put("map", map);
                if (in.get("default") != null) {
                    m.put("default", in.get("default"));
                }
                copyExtras(in, m);
                yield m;
            }
            default -> throw new IllegalArgumentException("Unsupported formula.type: " + type);
        };
    }

    public static BigDecimal compute(Object formula, BigDecimal amount) {
        return compute(formula, amount, null);
    }

    @SuppressWarnings("unchecked")
    public static BigDecimal compute(Object formula, BigDecimal amount, Map<String, String> metadata) {
        Map<String, Object> cfg = normalize(formula);
        String type = String.valueOf(cfg.get("type"));
        BigDecimal amt = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal base = switch (type) {
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
            case TYPE_TIERED_RATE -> scale(computeTiered(amt, cfg.get("brackets")));
            case TYPE_TABLE -> {
                Object nested = resolveTable(cfg, metadata);
                // avoid infinite table-in-table: compute nested without re-entering TABLE on same cfg
                yield compute(nested, amount, metadata);
            }
            default -> throw new IllegalArgumentException("Unsupported formula.type: " + type);
        };
        BigDecimal mult = decimal(cfg.get("multiplier"));
        if (mult == null) {
            mult = decimal(cfg.get("mult"));
        }
        if (mult != null && mult.compareTo(BigDecimal.ONE) != 0) {
            base = scale(base.multiply(mult));
        }
        BigDecimal floor = decimal(cfg.get("floor"));
        BigDecimal cap = decimal(cfg.get("cap"));
        if (cap == null) {
            cap = decimal(cfg.get("max"));
        }
        if (floor != null && base.compareTo(floor) < 0) {
            base = scale(floor);
        }
        if (cap != null && base.compareTo(cap) > 0) {
            base = scale(cap);
        }
        return base;
    }

    @SuppressWarnings("unchecked")
    private static BigDecimal computeTiered(BigDecimal amount, Object bracketsRaw) {
        if (!(bracketsRaw instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("TIERED_RATE brackets empty");
        }
        // marginal: sort by upTo ascending (null last = infinity)
        List<Map<String, Object>> brackets = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                brackets.add((Map<String, Object>) m);
            }
        }
        brackets.sort((a, b) -> {
            BigDecimal ua = decimal(first(a, "upTo", "to"));
            BigDecimal ub = decimal(first(b, "upTo", "to"));
            if (ua == null && ub == null) {
                return 0;
            }
            if (ua == null) {
                return 1;
            }
            if (ub == null) {
                return -1;
            }
            return ua.compareTo(ub);
        });
        BigDecimal remaining = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal prevUp = BigDecimal.ZERO;
        BigDecimal points = BigDecimal.ZERO;
        for (Map<String, Object> b : brackets) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal upTo = decimal(first(b, "upTo", "to"));
            BigDecimal rate = decimal(first(b, "rate", "value"));
            if (rate == null) {
                rate = BigDecimal.ZERO;
            }
            BigDecimal band;
            if (upTo == null) {
                band = remaining;
            } else {
                BigDecimal width = upTo.subtract(prevUp);
                if (width.signum() < 0) {
                    width = BigDecimal.ZERO;
                }
                band = remaining.min(width);
                prevUp = upTo;
            }
            points = points.add(band.multiply(rate));
            remaining = remaining.subtract(band);
        }
        return points;
    }

    @SuppressWarnings("unchecked")
    private static Object resolveTable(Map<String, Object> cfg, Map<String, String> metadata) {
        String by = String.valueOf(cfg.get("by"));
        String key = null;
        if (metadata != null) {
            if (by.startsWith("metadata.")) {
                key = metadata.get(by.substring("metadata.".length()));
            } else {
                key = metadata.get(by);
                if (key == null) {
                    for (var e : metadata.entrySet()) {
                        if (e.getKey() != null && e.getKey().equalsIgnoreCase(by)) {
                            key = e.getValue();
                            break;
                        }
                    }
                }
            }
        }
        Object mapObj = cfg.get("map");
        if (!(mapObj instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("TABLE map invalid");
        }
        Object chosen = null;
        if (key != null) {
            chosen = map.get(key);
            if (chosen == null) {
                chosen = map.get(key.toUpperCase(Locale.ROOT));
            }
            if (chosen == null) {
                chosen = map.get(key.toLowerCase(Locale.ROOT));
            }
        }
        if (chosen == null) {
            chosen = cfg.get("default");
        }
        if (chosen == null) {
            chosen = map.get("DEFAULT");
        }
        if (chosen == null) {
            chosen = map.get("*");
        }
        if (chosen == null) {
            throw new IllegalArgumentException("TABLE no case for key=" + key + " by=" + by);
        }
        return chosen;
    }

    public static boolean isSpendBased(Object formula) {
        try {
            Map<String, Object> cfg = normalize(formula);
            String t = String.valueOf(cfg.get("type"));
            return !TYPE_FIXED.equals(t);
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
            return normalize(JSONUtil.toMap(raw));
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

    private static void copyExtras(Map<String, Object> in, Map<String, Object> out) {
        BigDecimal mult = decimal(in.get("multiplier"));
        if (mult == null) {
            mult = decimal(in.get("mult"));
        }
        if (mult != null) {
            out.put("multiplier", mult);
        }
        BigDecimal floor = decimal(in.get("floor"));
        if (floor != null) {
            out.put("floor", floor);
        }
        BigDecimal cap = decimal(in.get("cap"));
        if (cap == null) {
            cap = decimal(in.get("max"));
        }
        if (cap != null) {
            out.put("cap", cap);
        }
    }

    /** @deprecated use {@link #copyExtras} */
    private static void copyMultiplier(Map<String, Object> in, Map<String, Object> out) {
        copyExtras(in, out);
    }
}
