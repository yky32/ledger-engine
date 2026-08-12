package com.altech.ledger.usecase.digestion;

import com.altech.ledger.usecase.digestion.TransactionRuleEngine.DigestionFormula;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DigestionFormulaTest {

    @Test
    void rate() {
        assertThat(DigestionFormula.compute("RATE:0.01", new BigDecimal("200")))
            .isEqualByComparingTo("2.000000000000000000");
    }

    @Test
    void fixed() {
        assertThat(DigestionFormula.compute("FIXED:100", BigDecimal.ZERO))
            .isEqualByComparingTo("100.000000000000000000");
    }

    @Test
    void amount() {
        assertThat(DigestionFormula.compute("AMOUNT", new BigDecimal("12.5")))
            .isEqualByComparingTo("12.500000000000000000");
    }

    @Test
    void mulAdd() {
        assertThat(DigestionFormula.compute("MUL_ADD:0.01:5", new BigDecimal("200")))
            .isEqualByComparingTo("7.000000000000000000");
    }

    @Test
    void jsonRateFixed() {
        assertThat(DigestionFormula.compute("{\"rate\":0.02,\"fixed\":1}", new BigDecimal("100")))
            .isEqualByComparingTo("3.000000000000000000");
    }

    @Test
    void badFormula() {
        assertThatThrownBy(() -> DigestionFormula.compute("WEIRD", BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
