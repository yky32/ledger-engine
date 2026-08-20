package com.altech.ledger.usecase.factor;

import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.usecase.digestion.DigestionRuleUseCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Common factor matcher for Door (entry) and Brain (when / equation selection).
 * <p>
 * Ops: eq, neq, in, nin, gt, gte, lt, lte, between, exists.
 * Fields: currency, mcc, amount, ageDays, eventType, metadata.*
 */
@Component
public class FactorMatcher {

    public record MatchResult(
        boolean matched,
        String failField,
        String failOp,
        String detail,
        List<String> path
    ) {
        public static MatchResult ok() {
            return ok(List.of());
        }

        public static MatchResult ok(List<String> path) {
            return new MatchResult(true, null, null, null,
                path == null ? List.of() : List.copyOf(path));
        }

        public static MatchResult fail(String field, String op, String detail) {
            return new MatchResult(false, field, op, detail, List.of());
        }

        public MatchResult prefix(String segment) {
            if (segment == null || segment.isBlank()) {
                return this;
            }
            List<String> p = new ArrayList<>();
            p.add(segment);
            if (path != null) {
                p.addAll(path);
            }
            return new MatchResult(matched, failField, failOp, detail, List.copyOf(p));
        }

        public String pathJoined() {
            if (path == null || path.isEmpty()) {
                return null;
            }
            return String.join(" > ", path);
        }

        /** failStep token for eligibilityTrace */
        public String failStep() {
            if (matched) {
                return null;
            }
            if (failField == null) {
                return "FACTOR";
            }
            String fl = failField.toLowerCase(Locale.ROOT);
            if (fl.equals("currency") || fl.equals("ccy")) {
                return "CURRENCY";
            }
            if (fl.equals("mcc")) {
                return "MCC";
            }
            if (fl.equals("amount") || fl.equals("amt")) {
                return "AMOUNT";
            }
            if (fl.equals("agedays") || fl.equals("age_days") || fl.equals("age")) {
                return "AGE";
            }
            if (fl.equals("eventtype") || fl.equals("event_type") || fl.equals("type")) {
                return "EVENT_TYPE";
            }
            if (fl.startsWith("metadata.") || fl.startsWith("meta.")) {
                return "META";
            }
            if (fl.equals("set") || fl.startsWith("set")) {
                return "SET";
            }
            return failField.toUpperCase(Locale.ROOT);
        }
    }

    /**
     * Evaluate factor spec: plain list (AND) or FactorSet object.
     * Empty / null → match.
     */
    public MatchResult matchAll(TransactionalEvent event, Object factorsOrSet) {
        return matchSpec(event, factorsOrSet);
    }

    /** @deprecated use {@link #matchAll(TransactionalEvent, Object)} */
    public MatchResult matchAll(TransactionalEvent event, List<Map<String, Object>> factors) {
        return matchSpec(event, factors);
    }

    @SuppressWarnings("unchecked")
    public MatchResult matchSpec(TransactionalEvent event, Object raw) {
        Object norm = FactorSpec.normalizeSpec(raw);
        if (norm == null) {
            return MatchResult.ok();
        }
        if (norm instanceof Map<?, ?> map) {
            return matchNode(event, (Map<String, Object>) map);
        }
        return MatchResult.ok();
    }

    @SuppressWarnings("unchecked")
    public MatchResult matchNode(TransactionalEvent event, Map<String, Object> node) {
        if (node == null || node.isEmpty()) {
            return MatchResult.ok();
        }
        if (FactorSpec.isSetNode(node)) {
            return matchSet(event, node);
        }
        if (FactorSpec.isLeaf(node)) {
            return matchLeaf(event, node);
        }
        // bare group with only factors nested
        if (node.containsKey("factors") || node.containsKey("items") || node.containsKey("groups")) {
            return matchSet(event, node);
        }
        return MatchResult.fail("factor", "set", "unrecognized factor node: " + node.keySet());
    }

    @SuppressWarnings("unchecked")
    public MatchResult matchSet(TransactionalEvent event, Map<String, Object> set) {
        String mode = FactorSpec.matchModeOf(set);
        List<Object> children = FactorSpec.childrenOf(set);
        List<Object> groups = FactorSpec.groupsOf(set);
        String setId = FactorSpec.idOf(set);

        return switch (mode) {
            case "all" -> {
                List<Object> nodes = !children.isEmpty() ? children : groups;
                List<String> pathAcc = new ArrayList<>();
                for (Object c : nodes) {
                    if (!(c instanceof Map<?, ?>)) {
                        continue;
                    }
                    MatchResult r = matchNode(event, (Map<String, Object>) c);
                    if (!r.matched()) {
                        yield setId == null ? r : r.prefix(setId);
                    }
                    if (r.path() != null) {
                        pathAcc.addAll(r.path());
                    }
                }
                yield setId == null ? MatchResult.ok(pathAcc) : MatchResult.ok(pathAcc).prefix(setId);
            }
            case "any" -> {
                List<Object> nodes = !children.isEmpty() ? children : groups;
                if (nodes.isEmpty()) {
                    yield MatchResult.ok(setId == null ? List.of() : List.of(setId));
                }
                List<String> fails = new ArrayList<>();
                for (Object c : nodes) {
                    if (!(c instanceof Map<?, ?>)) {
                        continue;
                    }
                    MatchResult r = matchNode(event, (Map<String, Object>) c);
                    if (r.matched()) {
                        yield setId == null ? r : r.prefix(setId);
                    }
                    fails.add(r.detail() == null ? r.failStep() : r.detail());
                }
                yield MatchResult.fail("SET", "any",
                    "none of " + nodes.size() + " factors matched; samples=" + fails.stream().limit(3).toList());
            }
            case "not", "none" -> {
                List<Object> nodes = !children.isEmpty() ? children : groups;
                // NOT: none of children may match (if empty → ok)
                for (Object c : nodes) {
                    if (!(c instanceof Map<?, ?>)) {
                        continue;
                    }
                    MatchResult r = matchNode(event, (Map<String, Object>) c);
                    if (r.matched()) {
                        yield MatchResult.fail("SET", "not",
                            "NOT failed; child matched " + (r.pathJoined() == null ? r.detail() : r.pathJoined()));
                    }
                }
                yield MatchResult.ok(setId == null ? List.of("NOT") : List.of(setId, "NOT"));
            }
            case "atLeast" -> countMode(event, set, children, groups, setId, false, false);
            case "exactly", "exact" -> countMode(event, set, children, groups, setId, true, false);
            case "atMost" -> countMode(event, set, children, groups, setId, false, true);
            case "anyGroup" -> {
                List<Object> gs = !groups.isEmpty() ? groups : children;
                if (gs.isEmpty()) {
                    yield MatchResult.ok();
                }
                List<String> fails = new ArrayList<>();
                for (Object g : gs) {
                    if (!(g instanceof Map<?, ?> gm)) {
                        continue;
                    }
                    MatchResult r = matchGroup(event, (Map<String, Object>) gm);
                    if (r.matched()) {
                        yield setId == null ? r : r.prefix(setId);
                    }
                    String id = FactorSpec.idOf((Map<String, Object>) gm);
                    fails.add((id == null ? "?" : id) + ":" + (r.detail() == null ? r.failStep() : r.detail()));
                }
                yield MatchResult.fail("SET", "anyGroup",
                    "no group matched; " + fails.stream().limit(4).toList());
            }
            case "allGroups" -> {
                List<Object> gs = !groups.isEmpty() ? groups : children;
                List<String> pathAcc = new ArrayList<>();
                for (Object g : gs) {
                    if (!(g instanceof Map<?, ?> gm)) {
                        continue;
                    }
                    MatchResult r = matchGroup(event, (Map<String, Object>) gm);
                    if (!r.matched()) {
                        yield setId == null ? r : r.prefix(setId);
                    }
                    if (r.path() != null) {
                        pathAcc.addAll(r.path());
                    }
                }
                yield setId == null ? MatchResult.ok(pathAcc) : MatchResult.ok(pathAcc).prefix(setId);
            }
            case "oneOf", "xor" -> {
                // exactly one child/group matches
                List<Object> nodes = !groups.isEmpty() ? groups : children;
                if (nodes.isEmpty()) {
                    yield MatchResult.ok();
                }
                int hit = 0;
                MatchResult lastHit = null;
                List<String> hitIds = new ArrayList<>();
                int i = 0;
                for (Object c : nodes) {
                    i++;
                    if (!(c instanceof Map<?, ?> m)) {
                        continue;
                    }
                    MatchResult r = matchNode(event, (Map<String, Object>) m);
                    String id = FactorSpec.idOf((Map<String, Object>) m);
                    if (id == null) {
                        id = "#" + i;
                    }
                    if (r.matched()) {
                        hit++;
                        hitIds.add(id);
                        lastHit = r.prefix(id);
                    }
                }
                if (hit == 1 && lastHit != null) {
                    yield setId == null ? lastHit : lastHit.prefix(setId);
                }
                yield MatchResult.fail("SET", "oneOf",
                    "oneOf needs exactly 1 match; got " + hit + " hit=" + hitIds);
            }
            default -> MatchResult.fail("SET", mode, "unsupported match mode: " + mode);
        };
    }

    @SuppressWarnings("unchecked")
    private MatchResult countMode(
        TransactionalEvent event,
        Map<String, Object> set,
        List<Object> children,
        List<Object> groups,
        String setId,
        boolean exactly,
        boolean atMost
    ) {
        List<Object> nodes = !children.isEmpty() ? children : groups;
        int need = FactorSpec.countOf(set, 1);
        if (nodes.isEmpty()) {
            if (exactly) {
                return need == 0 ? MatchResult.ok() : MatchResult.fail("SET", "exactly", "no factors; need " + need);
            }
            if (atMost) {
                return MatchResult.ok();
            }
            return need <= 0 ? MatchResult.ok() : MatchResult.fail("SET", "atLeast", "no factors; need " + need);
        }
        int hit = 0;
        List<String> hitIds = new ArrayList<>();
        List<String> hitPath = new ArrayList<>();
        List<String> miss = new ArrayList<>();
        int i = 0;
        for (Object c : nodes) {
            i++;
            if (!(c instanceof Map<?, ?> m)) {
                continue;
            }
            MatchResult r = matchNode(event, (Map<String, Object>) m);
            String id = FactorSpec.idOf((Map<String, Object>) m);
            if (id == null) {
                id = "#" + i;
            }
            if (r.matched()) {
                hit++;
                hitIds.add(id);
                hitPath.add(id);
                if (r.path() != null) {
                    hitPath.addAll(r.path());
                }
            } else {
                miss.add(id);
            }
        }
        boolean ok;
        String modeName;
        if (exactly) {
            ok = hit == need;
            modeName = "exactly";
        } else if (atMost) {
            ok = hit <= need;
            modeName = "atMost";
        } else {
            ok = hit >= need;
            modeName = "atLeast";
        }
        if (ok) {
            MatchResult r = MatchResult.ok(hitPath);
            return setId == null ? r : r.prefix(setId);
        }
        return MatchResult.fail("SET", modeName,
            modeName + " matched " + hit + " of " + nodes.size() + " (need " + need + "); hit=" + hitIds + " miss=" + miss);
    }

    /** Group defaults to AND of its factors (or nested match). */
    @SuppressWarnings("unchecked")
    private MatchResult matchGroup(TransactionalEvent event, Map<String, Object> group) {
        if (group == null) {
            return MatchResult.ok();
        }
        if (FactorSpec.isLeaf(group)) {
            return matchLeaf(event, group);
        }
        // ensure group without match still ANDs children
        if (!group.containsKey("match") && !group.containsKey("mode") && !group.containsKey("require")) {
            Map<String, Object> copy = new LinkedHashMap<>(group);
            copy.putIfAbsent("match", "all");
            if (!copy.containsKey("factors") && !copy.containsKey("items")
                && !copy.containsKey("rules") && !copy.containsKey("groups")) {
                // maybe the group IS a single nested structure already
                return matchNode(event, copy);
            }
            return matchSet(event, copy);
        }
        return matchNode(event, group);
    }

    public MatchResult matchOne(TransactionalEvent event, Map<String, Object> factor) {
        if (factor != null && FactorSpec.isSetNode(factor)) {
            return matchSet(event, factor);
        }
        return matchLeaf(event, factor);
    }

    public MatchResult matchLeaf(TransactionalEvent event, Map<String, Object> factor) {
        String field = FactorSpec.fieldOf(factor);
        String op = FactorSpec.opOf(factor);
        Object expected = FactorSpec.valueOf(factor);
        String leafId = FactorSpec.idOf(factor);
        if (leafId == null && field != null) {
            leafId = field + ":" + op;
        }
        if (field == null || field.isBlank()) {
            return MatchResult.fail("factor", op, "factor.field required");
        }
        String f = field.trim();

        try {
            MatchResult r;
            if ("exists".equals(op)) {
                Object actual = resolve(event, f);
                boolean want = expected == null || asBool(expected, true);
                boolean has = actual != null && !(actual instanceof String s && s.isBlank());
                r = has == want
                    ? MatchResult.ok()
                    : MatchResult.fail(f, op, "exists=" + has + " want=" + want);
            } else {
                Object actual = resolve(event, f);
                r = switch (op) {
                    case "eq", "=", "==" -> eq(f, op, actual, expected);
                    case "neq", "ne", "!=", "<>" -> {
                        MatchResult eq = eq(f, "eq", actual, expected);
                        yield eq.matched()
                            ? MatchResult.fail(f, op, "value equals " + expected)
                            : MatchResult.ok();
                    }
                    case "in" -> in(f, op, actual, expected, true);
                    case "nin", "not_in", "notin" -> in(f, op, actual, expected, false);
                    case "gt", ">" -> cmp(f, op, actual, expected, 1, false);
                    case "gte", "ge", ">=" -> cmp(f, op, actual, expected, 1, true);
                    case "lt", "<" -> cmp(f, op, actual, expected, -1, false);
                    case "lte", "le", "<=" -> cmp(f, op, actual, expected, -1, true);
                    case "between" -> between(f, op, actual, expected);
                    default -> MatchResult.fail(f, op, "unsupported op: " + op);
                };
            }
            if (r.matched() && leafId != null) {
                return r.path() != null && !r.path().isEmpty() ? r : MatchResult.ok(List.of(leafId));
            }
            return r;
        } catch (RuntimeException ex) {
            return MatchResult.fail(f, op, ex.getMessage() == null ? "factor error" : ex.getMessage());
        }
    }

    /**
     * Compile legacy Brain columns into factor list (always applied with whenFactors).
     */
    public List<Map<String, Object>> legacyWhenFactors(DigestionRule rule) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rule == null) {
            return out;
        }
        if (rule.getMinAmount() != null && rule.getMinAmount().signum() > 0) {
            out.add(factor("amount", "gte", rule.getMinAmount()));
        }
        List<String> ccys = DigestionRuleUseCase.splitCodes(rule.getEligibleCurrencies());
        if (!ccys.isEmpty()) {
            out.add(factor("currency", "in", ccys));
        }
        List<String> mccs = DigestionRuleUseCase.splitCodes(rule.getEligibleMccs());
        if (!mccs.isEmpty()) {
            out.add(factor("mcc", "in", mccs));
        }
        if (rule.getMaxAgeDays() != null) {
            out.add(factor("ageDays", "lte", rule.getMaxAgeDays()));
        }
        return out;
    }

    /**
     * Effective Brain gates: legacy AND explicit whenFactors (list or FactorSet).
     */
    public Object effectiveWhenFactors(DigestionRule rule) {
        List<Map<String, Object>> legacy = legacyWhenFactors(rule);
        Object explicit = rule == null ? null : rule.getWhenFactors();
        if (legacy.isEmpty()) {
            return explicit;
        }
        if (explicit == null || FactorSpec.normalizeSpec(explicit) == null) {
            return legacy;
        }
        // AND: all legacy leaves + entire explicit set as one child
        Map<String, Object> wrap = new LinkedHashMap<>();
        wrap.put("match", "all");
        List<Object> kids = new ArrayList<>(legacy);
        kids.add(explicit instanceof Map || explicit instanceof List
            ? FactorSpec.normalizeSpec(explicit)
            : explicit);
        wrap.put("factors", kids);
        return wrap;
    }

    private static Map<String, Object> factor(String field, String op, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("op", op);
        m.put("value", value);
        return m;
    }

    // ---- leaf helpers below (eq/in/cmp/between/resolve) ----

    public Object resolve(TransactionalEvent event, String field) {
        if (event == null || field == null) {
            return null;
        }
        String f = field.trim();
        String fl = f.toLowerCase(Locale.ROOT);
        if (fl.startsWith("metadata.") || fl.startsWith("meta.")) {
            int dot = f.indexOf('.');
            String key = f.substring(dot + 1);
            return meta(event, key);
        }
        return switch (fl) {
            case "currency", "ccy" -> event.currency() == null ? null
                : event.currency().getIsoCode().toUpperCase(Locale.ROOT);
            case "mcc" -> extractMcc(event);
            case "amount", "amt" -> event.amount();
            case "agedays", "age_days", "age" -> ageDays(event);
            case "eventtype", "event_type", "type" -> event.eventType() == null ? null
                : event.eventType().toUpperCase(Locale.ROOT);
            case "ownerid", "owner_id" -> event.ownerId();
            case "eventid", "event_id" -> event.eventId();
            default -> meta(event, f);
        };
    }

    private MatchResult eq(String field, String op, Object actual, Object expected) {
        if (actual == null && expected == null) {
            return MatchResult.ok();
        }
        if (actual == null || expected == null) {
            return MatchResult.fail(field, op, "actual=" + actual + " expected=" + expected);
        }
        if (actual instanceof BigDecimal || expected instanceof Number || expected instanceof BigDecimal) {
            BigDecimal a = toDecimal(actual);
            BigDecimal e = toDecimal(expected);
            if (a != null && e != null && a.compareTo(e) == 0) {
                return MatchResult.ok();
            }
            return MatchResult.fail(field, op, "actual=" + actual + " expected=" + expected);
        }
        String a = normStr(actual);
        String e = normStr(expected);
        if (a.equals(e)) {
            return MatchResult.ok();
        }
        return MatchResult.fail(field, op, "actual=" + a + " expected=" + e);
    }

    private MatchResult in(String field, String op, Object actual, Object expected, boolean mustContain) {
        Set<String> set = toStringSet(expected);
        if (set.isEmpty()) {
            return MatchResult.fail(field, op, "empty list");
        }
        if (actual == null) {
            return MatchResult.fail(field, op, "actual is null; allowed=" + set);
        }
        String a = normStr(actual);
        boolean hit = set.contains(a);
        if (mustContain == hit) {
            return MatchResult.ok();
        }
        return MatchResult.fail(field, op,
            mustContain
                ? ("actual=" + a + " not in " + set)
                : ("actual=" + a + " is in denied list " + set));
    }

    private MatchResult cmp(String field, String op, Object actual, Object expected, int wantSign, boolean eqOk) {
        BigDecimal a = toDecimal(actual);
        BigDecimal e = toDecimal(expected);
        if (a == null) {
            return MatchResult.fail(field, op, "actual not numeric: " + actual);
        }
        if (e == null) {
            return MatchResult.fail(field, op, "expected not numeric: " + expected);
        }
        int c = a.compareTo(e);
        boolean ok = c == wantSign || (eqOk && c == 0);
        if (ok) {
            return MatchResult.ok();
        }
        return MatchResult.fail(field, op, "actual=" + a + " op=" + op + " expected=" + e);
    }

    @SuppressWarnings("unchecked")
    private MatchResult between(String field, String op, Object actual, Object expected) {
        BigDecimal a = toDecimal(actual);
        if (a == null) {
            return MatchResult.fail(field, op, "actual not numeric: " + actual);
        }
        BigDecimal min = null;
        BigDecimal max = null;
        if (expected instanceof Map<?, ?> map) {
            min = toDecimal(map.get("min") != null ? map.get("min") : map.get("from"));
            max = toDecimal(map.get("max") != null ? map.get("max") : map.get("to"));
        } else if (expected instanceof Collection<?> col) {
            List<?> list = new ArrayList<>(col);
            if (list.size() >= 1) {
                min = toDecimal(list.get(0));
            }
            if (list.size() >= 2) {
                max = toDecimal(list.get(1));
            }
        } else if (expected instanceof String s && s.contains(",")) {
            String[] p = s.split(",", 2);
            min = toDecimal(p[0]);
            max = toDecimal(p[1]);
        }
        if (min == null && max == null) {
            return MatchResult.fail(field, op, "between needs min/max");
        }
        if (min != null && a.compareTo(min) < 0) {
            return MatchResult.fail(field, op, "actual=" + a + " < min " + min);
        }
        if (max != null && a.compareTo(max) > 0) {
            return MatchResult.fail(field, op, "actual=" + a + " > max " + max);
        }
        return MatchResult.ok();
    }

    private static BigDecimal ageDays(TransactionalEvent event) {
        if (event.occurredAt() == null) {
            return null;
        }
        long seconds = Duration.between(event.occurredAt(), Instant.now()).getSeconds();
        // fractional days as decimal days
        return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(86400L), 6, java.math.RoundingMode.HALF_UP);
    }

    private static String extractMcc(TransactionalEvent event) {
        if (event == null || event.metadata() == null) {
            return null;
        }
        for (String key : List.of("mcc", "mccCode", "merchantCategoryCode", "MCC")) {
            String v = event.metadata().get(key);
            if (v != null && !v.isBlank()) {
                return v.trim().toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    private static String meta(TransactionalEvent event, String key) {
        if (event == null || event.metadata() == null || key == null) {
            return null;
        }
        String v = event.metadata().get(key);
        if (v != null) {
            return v;
        }
        // case-insensitive key
        for (var e : event.metadata().entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String normStr(Object o) {
        return String.valueOf(o).trim().toUpperCase(Locale.ROOT);
    }

    private static Set<String> toStringSet(Object expected) {
        if (expected == null) {
            return Set.of();
        }
        if (expected instanceof Collection<?> col) {
            return col.stream()
                .filter(x -> x != null && !String.valueOf(x).isBlank())
                .map(FactorMatcher::normStr)
                .collect(Collectors.toSet());
        }
        if (expected instanceof String s) {
            if (s.contains(",")) {
                return DigestionRuleUseCase.splitCodes(s).stream().collect(Collectors.toSet());
            }
            return Set.of(normStr(s));
        }
        return Set.of(normStr(expected));
    }

    private static BigDecimal toDecimal(Object o) {
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

    private static boolean asBool(Object o, boolean dflt) {
        if (o == null) {
            return dflt;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(o).trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return dflt;
        }
        return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("y");
    }
}
