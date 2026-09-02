package com.altech.ledger.usecase.factor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared factor leaf + FactorSet boolean composition (UAF-ready).
 *
 * @see FactorMatcher
 * @see docs/BOOKLET.md (Factors)
 */
public final class FactorSpec {

    private FactorSpec() {}

    public static String fieldOf(Map<String, Object> f) {
        if (f == null) {
            return null;
        }
        Object v = first(f, "field", "name");
        // don't treat set "name"/"id" as field when it's a group
        if (f.containsKey("match") || f.containsKey("mode") || f.containsKey("groups")
            || f.containsKey("factors") || f.containsKey("items") || f.containsKey("rules")
            || f.containsKey("anyOf") || f.containsKey("allOf")) {
            if (!f.containsKey("field") && f.containsKey("name")) {
                return null;
            }
        }
        if (!f.containsKey("field") && f.containsKey("name")
            && (f.containsKey("op") || f.containsKey("operator") || f.containsKey("value"))) {
            return v == null ? null : String.valueOf(v).trim();
        }
        if (f.containsKey("field")) {
            return v == null ? null : String.valueOf(v).trim();
        }
        return null;
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

    /** FactorSet node (boolean tree), not a leaf field predicate. */
    public static boolean isSetNode(Map<String, Object> m) {
        if (m == null || m.isEmpty()) {
            return false;
        }
        if (m.containsKey("match") || m.containsKey("mode") || m.containsKey("require")
            || m.containsKey("groups") || m.containsKey("anyOf") || m.containsKey("allOf")
            || m.containsKey("atLeast") || m.containsKey("minMatch")) {
            return true;
        }
        boolean hasChildren = m.containsKey("factors") || m.containsKey("items") || m.containsKey("rules");
        boolean hasField = m.containsKey("field");
        return hasChildren && !hasField;
    }

    public static boolean isLeaf(Map<String, Object> m) {
        if (m == null || m.isEmpty() || isSetNode(m)) {
            return false;
        }
        String field = fieldOf(m);
        return field != null && !field.isBlank();
    }

    /**
     * Normalize stored JSON (array | set object | null).
     * Plain array → {@code {match:all, factors:[...]}}.
     */
    @SuppressWarnings("unchecked")
    public static Object normalizeSpec(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof List<?> list) {
            if (list.isEmpty()) {
                return null;
            }
            Map<String, Object> set = new LinkedHashMap<>();
            set.put("match", "all");
            set.put("factors", asNodeList(list));
            return set;
        }
        if (raw instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return null;
    }

    public static List<Object> asNodeList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?>) {
                    out.add(o);
                }
            }
            return out;
        }
        return List.of();
    }

    /** Flatten leaf factors only (legacy helpers). */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> asFactorList(Object raw) {
        Object norm = normalizeSpec(raw);
        if (!(norm instanceof Map<?, ?> set)) {
            return List.of();
        }
        return childrenOf((Map<String, Object>) set).stream()
            .filter(Map.class::isInstance)
            .map(m -> (Map<String, Object>) m)
            .filter(FactorSpec::isLeaf)
            .toList();
    }

    public static String matchModeOf(Map<String, Object> set) {
        if (set == null) {
            return "all";
        }
        Object v = first(set, "match", "mode");
        if (v == null) {
            v = set.get("require");
        }
        if (v == null || String.valueOf(v).isBlank()) {
            if (set.containsKey("anyOf")) {
                return "any";
            }
            if (set.containsKey("allOf")) {
                return "all";
            }
            if (set.containsKey("groups")) {
                return "anyGroup";
            }
            if (set.get("atLeast") != null || set.get("minMatch") != null
                || (set.get("count") != null && (set.containsKey("factors") || set.containsKey("items")))) {
                return "atLeast";
            }
            return "all";
        }
        String m = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        return switch (m) {
            case "and", "all", "allof", "every" -> "all";
            case "or", "any", "anyof", "one", "oneof_any" -> "any";
            case "atleast", "at_least", "n_of", "nof", "count", "min", "minmatch" -> "atLeast";
            case "exactly", "exact", "exactly_n" -> "exactly";
            case "atmost", "at_most", "max" -> "atMost";
            case "not", "none", "nor" -> "not";
            case "oneof", "xor", "exclusive" -> "oneOf";
            case "anygroup", "any_group", "groups_or", "or_groups" -> "anyGroup";
            case "allgroup", "all_groups", "groups_and", "and_groups" -> "allGroups";
            default -> m;
        };
    }

    public static int countOf(Map<String, Object> set, int dflt) {
        if (set == null) {
            return dflt;
        }
        Object v = first(set, "count", "min");
        if (v == null) {
            v = first(set, "atLeast", "minMatch");
        }
        if (v == null) {
            return dflt;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException ex) {
            return dflt;
        }
    }

    public static List<Object> childrenOf(Map<String, Object> set) {
        if (set == null) {
            return List.of();
        }
        Object v = first(set, "factors", "items");
        if (v == null) {
            v = first(set, "rules", "anyOf");
        }
        if (v == null) {
            v = set.get("allOf");
        }
        return asNodeList(v);
    }

    public static List<Object> groupsOf(Map<String, Object> set) {
        if (set == null) {
            return List.of();
        }
        Object v = first(set, "groups", "anyGroup");
        if (v == null) {
            v = set.get("group");
        }
        return asNodeList(v);
    }

    public static String idOf(Map<String, Object> node) {
        if (node == null) {
            return null;
        }
        Object v = first(node, "id", "name");
        if (v == null) {
            v = node.get("code");
        }
        return v == null ? null : String.valueOf(v);
    }

    private static Object first(Map<String, Object> m, String a, String b) {
        if (m.containsKey(a) && m.get(a) != null) {
            return m.get(a);
        }
        return m.get(b);
    }
}
