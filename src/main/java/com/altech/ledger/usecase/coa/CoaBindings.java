package com.altech.ledger.usecase.coa;

import com.altech.core.constant.enu.Currency;
import com.altech.core.utils.JSONUtil;
import com.altech.ledger.util.CoaCodes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * JSON bindings on {@code coa_profile}. Minimal 3 roles.
 */
public final class CoaBindings {
    public static final String ROLE_MEMBER_SETTLEMENT = "MEMBER_SETTLEMENT";
    public static final String ROLE_MEMBER_LP = "MEMBER_LP";
    public static final String ROLE_PROGRAM_POOL = "PROGRAM_POOL";

    public static final String MODE_SETTLEMENT = "SETTLEMENT";
    public static final String MODE_ENSURE = "ENSURE";
    public static final String MODE_FIXED = "FIXED";

    private CoaBindings() {}

    /** Default = today's CoaCodes hardcode (LIABILITY / entity 10 / …). */
    public static Map<String, Object> defaultBindings() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put(ROLE_MEMBER_SETTLEMENT, role(
            CoaCodes.ENTITY, CoaCodes.typeCodeLiability(), CoaCodes.SUB_TYPE, CoaCodes.BUFFER,
            MODE_SETTLEMENT, null, false));
        root.put(ROLE_MEMBER_LP, role(
            CoaCodes.ENTITY, CoaCodes.typeCodeLiability(), CoaCodes.SUB_TYPE, CoaCodes.BUFFER,
            MODE_FIXED, "LP", false));
        root.put(ROLE_PROGRAM_POOL, role(
            CoaCodes.ENTITY, CoaCodes.typeCodeLiability(), CoaCodes.SUB_TYPE, CoaCodes.BUFFER,
            MODE_FIXED, "LP", true));
        return root;
    }

    public static Map<String, Object> normalize(Object raw) {
        Map<String, Object> src = raw == null ? Map.of() : JSONUtil.toMap(raw);
        Map<String, Object> defaults = defaultBindings();
        Map<String, Object> out = new LinkedHashMap<>();
        for (String role : new String[] { ROLE_MEMBER_SETTLEMENT, ROLE_MEMBER_LP, ROLE_PROGRAM_POOL }) {
            Object node = src.get(role);
            Map<String, Object> base = JSONUtil.toMap(defaults.get(role));
            if (node instanceof Map<?, ?> m && !m.isEmpty()) {
                Map<String, Object> overlay = JSONUtil.toMap(node);
                base.putAll(overlay);
            }
            out.put(role, sanitizeRole(base));
        }
        return out;
    }

    public static RoleSegments require(Map<String, Object> bindings, String role) {
        Map<String, Object> normalized = normalize(bindings);
        Object node = normalized.get(role);
        if (node == null) {
            throw new IllegalArgumentException("COA role missing: " + role);
        }
        Map<String, Object> m = JSONUtil.toMap(node);
        return new RoleSegments(
            str(m.get("entity"), CoaCodes.ENTITY),
            str(m.get("type"), CoaCodes.typeCodeLiability()),
            str(m.get("subType"), CoaCodes.SUB_TYPE),
            str(m.get("buffer"), CoaCodes.BUFFER),
            str(m.get("currencyMode"), MODE_SETTLEMENT).toUpperCase(Locale.ROOT),
            m.get("currency") == null ? null : String.valueOf(m.get("currency")).trim().toUpperCase(Locale.ROOT),
            bool(m.get("allowNegative"), false)
        );
    }

    /** Pick member book role by currency (LP → MEMBER_LP). */
    public static RoleSegments forMemberCurrency(Map<String, Object> bindings, Currency currency) {
        if (currency == Currency.LP) {
            return require(bindings, ROLE_MEMBER_LP);
        }
        return require(bindings, ROLE_MEMBER_SETTLEMENT);
    }

    private static Map<String, Object> role(
        String entity, String type, String subType, String buffer,
        String currencyMode, String currency, boolean allowNegative
    ) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entity", entity);
        m.put("type", type);
        m.put("subType", subType);
        m.put("buffer", buffer);
        m.put("currencyMode", currencyMode);
        if (currency != null) {
            m.put("currency", currency);
        }
        m.put("allowNegative", allowNegative);
        return m;
    }

    private static Map<String, Object> sanitizeRole(Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("entity", digits(str(m.get("entity"), CoaCodes.ENTITY), CoaCodes.ENTITY));
        out.put("type", digits(str(m.get("type"), CoaCodes.typeCodeLiability()), CoaCodes.typeCodeLiability()));
        out.put("subType", digits(str(m.get("subType"), CoaCodes.SUB_TYPE), CoaCodes.SUB_TYPE));
        out.put("buffer", digits(str(m.get("buffer"), CoaCodes.BUFFER), CoaCodes.BUFFER));
        String mode = str(m.get("currencyMode"), MODE_SETTLEMENT).toUpperCase(Locale.ROOT);
        if (!MODE_SETTLEMENT.equals(mode) && !MODE_ENSURE.equals(mode) && !MODE_FIXED.equals(mode)) {
            mode = MODE_SETTLEMENT;
        }
        out.put("currencyMode", mode);
        if (m.get("currency") != null && !String.valueOf(m.get("currency")).isBlank()) {
            out.put("currency", String.valueOf(m.get("currency")).trim().toUpperCase(Locale.ROOT));
        }
        out.put("allowNegative", bool(m.get("allowNegative"), false));
        return out;
    }

    private static String digits(String v, String fallback) {
        if (v == null || !v.matches("\\d+")) {
            return fallback;
        }
        return v;
    }

    private static String str(Object o, String d) {
        if (o == null) {
            return d;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? d : s;
    }

    private static boolean bool(Object o, boolean d) {
        if (o instanceof Boolean b) {
            return b;
        }
        if (o == null) {
            return d;
        }
        return Boolean.parseBoolean(String.valueOf(o));
    }

    public record RoleSegments(
        String entity,
        String type,
        String subType,
        String buffer,
        String currencyMode,
        String currencyFixed,
        boolean allowNegative
    ) {}
}
