package com.altech.ledger.support;

import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.usecase.digestion.DigestionFormulaConfig;

import java.math.BigDecimal;
import java.util.Map;

/** Test-only default digestion rules (no YAML / no startup seed in app). */
public final class DigestionRuleTestData {
    private DigestionRuleTestData() {}

    public static void ensureDefaultRules(DigestionRuleRepository repo) {
        upsert(repo, "PURCHASE_IT", "PURCHASE", "EARN",
            DigestionFormulaConfig.ofRate(new BigDecimal("0.01")),
            new BigDecimal("0.01"), "HKD,USD", 7, 10);
        upsert(repo, "SIGNUP_IT", "SIGNUP", "EARN",
            DigestionFormulaConfig.ofFixed(new BigDecimal("100")),
            BigDecimal.ZERO, null, null, 20);
        upsert(repo, "REDEEM_IT", "REDEEM", "BURN",
            DigestionFormulaConfig.ofAmount(),
            BigDecimal.ONE, null, null, 30);
    }

    private static void upsert(
        DigestionRuleRepository repo,
        String code,
        String eventType,
        String operation,
        Map<String, Object> formula,
        BigDecimal minAmount,
        String eligible,
        Integer maxAgeDays,
        int priority
    ) {
        DigestionRule r = repo.findByCode(code).orElseGet(DigestionRule::new);
        r.setCode(code);
        r.setName("IT " + eventType);
        r.setEventType(eventType);
        r.setOperation(operation);
        r.setIsEnabled(true);
        r.setIsActive(true);
        r.setPriority(priority);
        r.setMinAmount(minAmount);
        r.setEligibleCurrencies(eligible);
        r.setMaxAgeDays(maxAgeDays);
        r.setPointCurrency("LP");
        r.setFormula(formula);
        repo.save(r);
    }
}
