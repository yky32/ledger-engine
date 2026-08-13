package com.altech.ledger.usecase.digestion;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.repository.DigestionRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionRuleEngineTest {

    @Mock DigestionRuleRepository repo;
    private TransactionRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TransactionRuleEngine(repo);
        DigestionRule purchase = new DigestionRule();
        purchase.setCode("PURCHASE_T");
        purchase.setEventType("PURCHASE");
        purchase.setOperation("EARN");
        purchase.setIsEnabled(true);
        purchase.setPriority(10);
        purchase.setMinAmount(new BigDecimal("0.01"));
        purchase.setPointCurrency("LP");
        purchase.setFormula(DigestionFormulaConfig.ofRate(new BigDecimal("0.01")));
        purchase.setMaxAgeDays(7);
        purchase.setEligibleCurrencies("HKD,USD");
        purchase.setIsActive(true);
        when(repo.findAllEnabledOrdered()).thenReturn(List.of(purchase));
    }

    @Test
    void matchesEligiblePurchase() {
        var event = new TransactionalEvent(
            "e1", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.HKD,
            Instant.now().minus(2, ChronoUnit.DAYS), Map.of());
        var out = engine.evaluate(event);
        assertThat(out.matched()).isTrue();
        assertThat(out.decision().orElseThrow().points()).isEqualByComparingTo("1.000000000000000000");
        assertThat(out.decision().orElseThrow().matchedRule()).isEqualTo("PURCHASE_T");
        assertThat(out.trace()).isNotEmpty();
        assertThat(out.trace().get(out.trace().size() - 1).matched()).isTrue();
    }

    @Test
    void rejectsIneligibleCurrencyWithTrace() {
        var event = new TransactionalEvent(
            "e2", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.JPY,
            Instant.now(), Map.of());
        var out = engine.evaluate(event);
        assertThat(out.matched()).isFalse();
        assertThat(out.skipReasonCode()).isEqualTo("CURRENCY");
        assertThat(out.trace()).hasSize(1);
        assertThat(out.trace().get(0).failStep()).isEqualTo("CURRENCY");
    }

    @Test
    void rejectsMissingOccurredAtWhenMaxAgeConfigured() {
        var event = new TransactionalEvent(
            "e3", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.HKD,
            null, Map.of());
        var out = engine.evaluate(event);
        assertThat(out.matched()).isFalse();
        assertThat(out.skipReasonCode()).isEqualTo("AGE");
        assertThat(out.trace().get(0).failStep()).isEqualTo("AGE");
    }

    @Test
    void rejectsTooOld() {
        var event = new TransactionalEvent(
            "e4", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.HKD,
            Instant.now().minus(10, ChronoUnit.DAYS), Map.of());
        var out = engine.evaluate(event);
        assertThat(out.matched()).isFalse();
        assertThat(out.skipReasonCode()).isEqualTo("AGE");
    }

    @Test
    void rejectsNonPositiveAmountForRateFormula() {
        var event = new TransactionalEvent(
            "e5", "01A12345678", "PURCHASE", BigDecimal.ZERO, Currency.HKD,
            Instant.now(), Map.of());
        var out = engine.evaluate(event);
        assertThat(out.matched()).isFalse();
        assertThat(out.skipReasonCode()).isEqualTo("AMOUNT");
    }

    @Test
    void rejectsIneligibleMccWhenConfigured() {
        DigestionRule grocery = new DigestionRule();
        grocery.setCode("GROCERY");
        grocery.setEventType("PURCHASE");
        grocery.setOperation("EARN");
        grocery.setIsEnabled(true);
        grocery.setPriority(5);
        grocery.setMinAmount(BigDecimal.ZERO);
        grocery.setPointCurrency("LP");
        grocery.setFormula(DigestionFormulaConfig.ofRate(new BigDecimal("0.03")));
        grocery.setEligibleCurrencies("HKD");
        grocery.setEligibleMccs("5411");
        grocery.setIsActive(true);
        when(repo.findAllEnabledOrdered()).thenReturn(List.of(grocery));

        var bad = new TransactionalEvent(
            "e6", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.HKD,
            Instant.now(), Map.of("mcc", "5812"));
        var badOut = engine.evaluate(bad);
        assertThat(badOut.skipReasonCode()).isEqualTo("MCC");
        assertThat(badOut.trace().get(0).failStep()).isEqualTo("MCC");

        var ok = new TransactionalEvent(
            "e7", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.HKD,
            Instant.now(), Map.of("mcc", "5411"));
        var out = engine.evaluate(ok);
        assertThat(out.matched()).isTrue();
        assertThat(out.decision().orElseThrow().points()).isEqualByComparingTo("3");
        assertThat(out.decision().orElseThrow().matchedRule()).isEqualTo("GROCERY");
    }

    @Test
    void rejectsMissingMccWhenAllowListSet() {
        DigestionRule grocery = new DigestionRule();
        grocery.setCode("GROCERY2");
        grocery.setEventType("PURCHASE");
        grocery.setOperation("EARN");
        grocery.setIsEnabled(true);
        grocery.setPriority(5);
        grocery.setMinAmount(BigDecimal.ZERO);
        grocery.setPointCurrency("LP");
        grocery.setFormula(DigestionFormulaConfig.ofRate(new BigDecimal("0.01")));
        grocery.setEligibleMccs("5411");
        grocery.setIsActive(true);
        when(repo.findAllEnabledOrdered()).thenReturn(List.of(grocery));

        var event = new TransactionalEvent(
            "e8", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.HKD,
            Instant.now(), Map.of());
        assertThat(engine.evaluate(event).skipReasonCode()).isEqualTo("MCC");
    }
}
