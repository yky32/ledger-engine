package com.altech.ledger.config;

import com.altech.ledger.entity.po.integration.DigestionRule;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.usecase.integration.DigestionRuleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Seeds {@code digestion_rule} from YAML {@code ledger.integration.rules} when table is empty.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class DigestionRuleYamlSeedRunner implements ApplicationRunner {
    private final DigestionRuleRepository digestionRuleRepository;
    private final IntegrationProperties integrationProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (digestionRuleRepository.count() > 0) {
            log.debug("digestion_rule already seeded (count={})", digestionRuleRepository.count());
            return;
        }
        if (integrationProperties.getRules() == null || integrationProperties.getRules().isEmpty()) {
            log.warn("No YAML ledger.integration.rules to seed into digestion_rule");
            return;
        }
        int i = 0;
        for (IntegrationProperties.TransactionRule yr : integrationProperties.getRules()) {
            if (yr.getEventType() == null || yr.getEventType().isBlank()) {
                continue;
            }
            i++;
            DigestionRule r = new DigestionRule();
            String event = yr.getEventType().trim().toUpperCase(Locale.ROOT);
            r.setCode(event + "_SEED_" + i);
            r.setName("YAML seed " + event);
            r.setEventType(event);
            r.setOperation(yr.getOperation() == null ? "EARN" : yr.getOperation().trim().toUpperCase(Locale.ROOT));
            r.setIsEnabled(true);
            r.setPriority(i * 10);
            r.setMinAmount(yr.getMinAmount() == null ? BigDecimal.ZERO : yr.getMinAmount());
            r.setEligibleCurrencies(DigestionRuleUseCase.joinCurrencies(yr.getEligibleCurrencies()));
            r.setMaxAgeDays(yr.getMaxAgeDays());
            r.setPointCurrency(yr.getPointCurrency() == null ? "LP" : yr.getPointCurrency());
            r.setFormula(yr.getFormula() == null ? "AMOUNT" : yr.getFormula());
            r.setProcessType(yr.getProcessType());
            r.setIsActive(true);
            digestionRuleRepository.save(r);
        }
        log.info("Seeded {} digestion_rule row(s) from YAML", i);
    }
}
