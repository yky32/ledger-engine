package com.altech.ledger.usecase.factor;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactorMatcherTest {

    private FactorMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new FactorMatcher();
    }

    private TransactionalEvent evt(String ccy, String amount, String mcc, Instant at) {
        Map<String, String> meta = new LinkedHashMap<>();
        if (mcc != null) {
            meta.put("mcc", mcc);
        }
        return new TransactionalEvent(
            "e1",
            "OWN1",
            "PURCHASE",
            new BigDecimal(amount),
            Currency.get(ccy),
            at,
            meta
        );
    }

    @Test
    void amountBetweenAndCurrencyIn() {
        var e = evt("HKD", "500", "5411", Instant.now());
        var factors = List.of(
            Map.<String, Object>of("field", "currency", "op", "in", "value", List.of("HKD", "USD")),
            Map.<String, Object>of("field", "amount", "op", "between", "value",
                Map.of("min", 100, "max", 1000)),
            Map.<String, Object>of("field", "mcc", "op", "eq", "value", "5411")
        );
        assertTrue(matcher.matchAll(e, factors).matched());
    }

    @Test
    void amountLtFails() {
        var e = evt("HKD", "50", null, Instant.now());
        var f = List.of(Map.<String, Object>of("field", "amount", "op", "gte", "value", 100));
        var r = matcher.matchAll(e, f);
        assertFalse(r.matched());
        assertTrue(r.detail().contains("100") || r.failStep().equals("AMOUNT"));
    }

    @Test
    void mccNin() {
        var e = evt("HKD", "10", "6010", Instant.now());
        var f = List.of(Map.<String, Object>of("field", "mcc", "op", "nin", "value", List.of("6010", "6011")));
        assertFalse(matcher.matchAll(e, f).matched());
        e = evt("HKD", "10", "5411", Instant.now());
        assertTrue(matcher.matchAll(e, f).matched());
    }

    @Test
    void ageLte() {
        var old = evt("HKD", "10", null, Instant.now().minus(40, ChronoUnit.DAYS));
        var f = List.of(Map.<String, Object>of("field", "ageDays", "op", "lte", "value", 30));
        assertFalse(matcher.matchAll(old, f).matched());
        var fresh = evt("HKD", "10", null, Instant.now().minus(2, ChronoUnit.DAYS));
        assertTrue(matcher.matchAll(fresh, f).matched());
    }

    @Test
    void emptyFactorsOk() {
        assertTrue(matcher.matchAll(evt("HKD", "1", null, Instant.now()), List.of()).matched());
        assertTrue(matcher.matchAll(evt("HKD", "1", null, Instant.now()), null).matched());
    }
}
