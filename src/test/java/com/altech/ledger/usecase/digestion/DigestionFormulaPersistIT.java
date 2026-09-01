
package com.altech.ledger.usecase.digestion;

import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.repository.DigestionRuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DigestionFormulaPersistIT {
    @Autowired DigestionRuleRepository repo;

    @Test
    void roundTripFormulaJson() {
        DigestionRule r = new DigestionRule();
        r.setCode("FORMULA_RT_" + System.nanoTime());
        r.setEventType("PURCHASE");
        r.setOperation("EARN");
        r.setIsEnabled(true);
        r.setIsActive(true);
        r.setPriority(1);
        r.setMinAmount(BigDecimal.ZERO);
        r.setResultCurrency("LP");
        r.setFormula(DigestionFormulaConfig.ofRate(new BigDecimal("0.01")));
        r = repo.saveAndFlush(r);
        repo.flush();
        DigestionRule loaded = repo.findById(r.getId()).orElseThrow();
        System.out.println("CLASS=" + loaded.getFormula().getClass().getName());
        System.out.println("VALUE=" + loaded.getFormula());
        System.out.println("KEYS=" + loaded.getFormula().keySet());
        BigDecimal pts = DigestionFormulaConfig.compute(loaded.getFormula(), new BigDecimal("200"));
        System.out.println("POINTS=" + pts);
        assertThat(pts).isEqualByComparingTo("2");
    }
}
