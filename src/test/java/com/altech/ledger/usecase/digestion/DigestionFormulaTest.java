package com.altech.ledger.usecase.digestion;

import com.altech.ledger.usecase.digestion.TransactionRuleEngine.DigestionFormula;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DigestionFormulaTest {

    @Test
    void jsonRate() {
        assertThat(DigestionFormula.compute(Map.of("type", "RATE", "rate", "0.01"), new BigDecimal("200")))
            .isEqualByComparingTo("2.000000000000000000");
    }

    @Test
    void jsonFixed() {
        assertThat(DigestionFormula.compute(Map.of("type", "FIXED", "value", 100), BigDecimal.ZERO))
            .isEqualByComparingTo("100.000000000000000000");
    }

    @Test
    void jsonAmount() {
        assertThat(DigestionFormula.compute(Map.of("type", "AMOUNT"), new BigDecimal("12.5")))
            .isEqualByComparingTo("12.500000000000000000");
    }

    @Test
    void jsonLinear() {
        assertThat(DigestionFormula.compute(
            Map.of("type", "LINEAR", "rate", "0.01", "fixed", "5"),
            new BigDecimal("200")))
            .isEqualByComparingTo("7.000000000000000000");
    }

    @Test
    void legacyStringStillWorks() {
        assertThat(DigestionFormula.compute("RATE:0.01", new BigDecimal("200")))
            .isEqualByComparingTo("2.000000000000000000");
        assertThat(DigestionFormula.compute("MUL_ADD:0.01:5", new BigDecimal("200")))
            .isEqualByComparingTo("7.000000000000000000");
    }

    @Test
    void bareRateFixedJson() {
        assertThat(DigestionFormula.compute("{\"rate\":0.02,\"fixed\":1}", new BigDecimal("100")))
            .isEqualByComparingTo("3.000000000000000000");
    }

    @Test
    void normalizeLegacy() {
        assertThat(DigestionFormulaConfig.normalize("FIXED:50"))
            .containsEntry("type", "FIXED");
    }

    @Test
    void badFormula() {
        assertThatThrownBy(() -> DigestionFormula.compute(Map.of("type", "WEIRD"), BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
