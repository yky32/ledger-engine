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
        purchase.setFormula("RATE:0.01");
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
    }

    @Test
    void rejectsIneligibleCurrency() {
        var event = new TransactionalEvent(
            "e2", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.JPY,
            Instant.now(), Map.of());
        var out = engine.evaluate(event);
        assertThat(out.matched()).isFalse();
        assertThat(out.skipReasonCode()).isEqualTo("CURRENCY");
    }

    @Test
    void rejectsMissingOccurredAtWhenMaxAgeConfigured() {
        var event = new TransactionalEvent(
            "e3", "01A12345678", "PURCHASE", new BigDecimal("100"), Currency.HKD,
            null, Map.of());
        var out = engine.evaluate(event);
        assertThat(out.matched()).isFalse();
        assertThat(out.skipReasonCode()).isEqualTo("AGE");
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
}
