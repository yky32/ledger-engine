package com.altech.ledger.usecase.factor;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.usecase.digestion.DigestionFormulaConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FactorAdvancedAtoETest {

    private final FactorMatcher matcher = new FactorMatcher();

    private static Map<String, Object> leaf(String id, String field, String op, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("field", field);
        m.put("op", op);
        m.put("value", value);
        return m;
    }

    private TransactionalEvent evt(String ccy, String amt, String mcc) {
        Map<String, String> meta = new LinkedHashMap<>();
        if (mcc != null) meta.put("mcc", mcc);
        meta.put("tier", "GOLD");
        return TransactionalEvent.of("e", "O1", "PURCHASE", new BigDecimal(amt), Currency.get(ccy), Instant.now(), meta);
    }

    @Test
    void explainPath_onAnyGroup() {
        Map<String, Object> set = new LinkedHashMap<>();
        set.put("match", "anyGroup");
        set.put("groups", List.of(
            Map.of("id", "G12", "factors", List.of(
                leaf("F1", "currency", "eq", "HKD"),
                leaf("F2", "mcc", "eq", "5411")
            )),
            Map.of("id", "G34", "factors", List.of(
                leaf("F3", "amount", "gte", 9000)
            ))
        ));
        var r = matcher.matchAll(evt("HKD", "100", "5411"), set);
        assertThat(r.matched()).isTrue();
        assertThat(r.pathJoined()).contains("G12");
    }

    @Test
    void not_and_exactly_and_oneOf() {
        var fHkd = leaf("H", "currency", "eq", "HKD");
        var fUsd = leaf("U", "currency", "eq", "USD");
        var notUsd = Map.of("match", "not", "factors", List.of(fUsd));
        assertThat(matcher.matchAll(evt("HKD", "1", null), notUsd).matched()).isTrue();
        assertThat(matcher.matchAll(evt("USD", "1", null), notUsd).matched()).isFalse();

        var exactly1 = Map.of("match", "exactly", "count", 1, "factors", List.of(fHkd, fUsd));
        assertThat(matcher.matchAll(evt("HKD", "1", null), exactly1).matched()).isTrue();

        var oneOf = Map.of("match", "oneOf", "factors", List.of(fHkd, fUsd));
        assertThat(matcher.matchAll(evt("HKD", "1", null), oneOf).matched()).isTrue();
        // both can't match same event
    }

    @Test
    void tieredRate_and_cap_floor() {
        Map<String, Object> b2 = new LinkedHashMap<>();
        b2.put("upTo", null);
        b2.put("rate", 0.02);
        Map<String, Object> tiered = new LinkedHashMap<>();
        tiered.put("type", "TIERED_RATE");
        tiered.put("brackets", List.of(
            Map.of("upTo", 1000, "rate", 0.01),
            b2
        ));
        // 1500 → 1000*0.01 + 500*0.02 = 10 + 10 = 20
        assertThat(DigestionFormulaConfig.compute(tiered, new BigDecimal("1500")))
            .isEqualByComparingTo("20.000000000000000000");

        Map<String, Object> capped = Map.of(
            "type", "RATE",
            "rate", 0.01,
            "cap", 5,
            "floor", 1
        );
        assertThat(DigestionFormulaConfig.compute(capped, new BigDecimal("1000")))
            .isEqualByComparingTo("5.000000000000000000");
        assertThat(DigestionFormulaConfig.compute(capped, new BigDecimal("10")))
            .isEqualByComparingTo("1.000000000000000000");
    }

    @Test
    void table_by_tier() {
        Map<String, Object> table = Map.of(
            "type", "TABLE",
            "by", "tier",
            "map", Map.of(
                "GOLD", Map.of("type", "RATE", "rate", 0.02),
                "DEFAULT", Map.of("type", "RATE", "rate", 0.01)
            )
        );
        assertThat(DigestionFormulaConfig.compute(table, new BigDecimal("100"), Map.of("tier", "GOLD")))
            .isEqualByComparingTo("2.000000000000000000");
    }
}
