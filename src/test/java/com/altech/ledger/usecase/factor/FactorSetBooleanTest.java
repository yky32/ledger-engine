package com.altech.ledger.usecase.factor;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UAF-style composition: any / atLeast N / specific combinations.
 */
class FactorSetBooleanTest {

    private FactorMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new FactorMatcher();
    }

    private static Map<String, Object> f(String field, String op, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", field);
        m.put("op", op);
        m.put("value", value);
        if (field != null) {
            m.put("id", field + ":" + op);
        }
        return m;
    }

    private TransactionalEvent evt(String ccy, String amount, String mcc) {
        Map<String, String> meta = new LinkedHashMap<>();
        if (mcc != null) {
            meta.put("mcc", mcc);
        }
        meta.put("channel", "POS");
        return new TransactionalEvent(
            "e1", "OWN1", "PURCHASE", new BigDecimal(amount), Currency.get(ccy), Instant.now(), meta);
    }

    private final Map<String, Object> f1 = f("currency", "eq", "HKD");
    private final Map<String, Object> f2 = f("mcc", "in", List.of("5411", "5812"));
    private final Map<String, Object> f3 = f("amount", "gte", 500);
    private final Map<String, Object> f4 = f("amount", "between", Map.of("min", 500, "max", 10000));
    private final Map<String, Object> f5 = f("metadata.channel", "eq", "POS");

    @Test
    void any_oneOfFive() {
        Map<String, Object> set = Map.of(
            "match", "any",
            "factors", List.of(f1, f2, f3, f4, f5)
        );
        // channel POS hits f5
        assertTrue(matcher.matchAll(evt("USD", "1", "9999"), set).matched());
        // nothing matches
        var bare = new TransactionalEvent(
            "e2", "OWN1", "PURCHASE", new BigDecimal("1"), Currency.USD, Instant.now(), Map.of());
        assertFalse(matcher.matchAll(bare, set).matched());
    }

    @Test
    void atLeast_twoOfFive() {
        Map<String, Object> set = new LinkedHashMap<>();
        set.put("match", "atLeast");
        set.put("count", 2);
        set.put("factors", List.of(f1, f2, f3, f4, f5));
        // HKD + grocery 600 → f1,f2,f3,f4,f5
        assertTrue(matcher.matchAll(evt("HKD", "600", "5411"), set).matched());
        // only channel POS (1 hit) → fail need 2
        var bare = new TransactionalEvent(
            "e3", "OWN1", "PURCHASE", new BigDecimal("1"), Currency.USD, Instant.now(),
            Map.of("channel", "POS"));
        assertFalse(matcher.matchAll(bare, set).matched());
    }

    @Test
    void anyGroup_combo_1and2_or_3and4() {
        Map<String, Object> g12 = new LinkedHashMap<>();
        g12.put("id", "G12");
        g12.put("factors", List.of(f1, f2));
        Map<String, Object> g34 = new LinkedHashMap<>();
        g34.put("id", "G34");
        g34.put("factors", List.of(f3, f4));
        Map<String, Object> set = new LinkedHashMap<>();
        set.put("match", "anyGroup");
        set.put("groups", List.of(g12, g34));

        // HKD grocery 100 → G12 (ccy+mcc), amount not >=500 so G34 fail — still ok via G12
        assertTrue(matcher.matchAll(evt("HKD", "100", "5411"), set).matched());
        // USD amount 800 mcc junk → G12 fail, G34 ok (800 between 500-10000)
        assertTrue(matcher.matchAll(evt("USD", "800", "9999"), set).matched());
        // USD 50 junk mcc → both fail
        assertFalse(matcher.matchAll(evt("USD", "50", "9999"), set).matched());
    }

    @Test
    void plainArrayStillAnd() {
        assertTrue(matcher.matchAll(evt("HKD", "600", "5411"), List.of(f1, f3)).matched());
        assertFalse(matcher.matchAll(evt("USD", "600", "5411"), List.of(f1, f3)).matched());
    }
}
