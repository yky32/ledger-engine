package com.altech.ledger.usecase.integration;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class TransactionRuleEngine {
    public enum Operation { EARN, BURN, PROCESS }

    private final IntegrationProperties properties;

    public TransactionRuleEngine(IntegrationProperties properties) {
        this.properties = properties;
    }

    public Optional<RuleDecision> evaluate(TransactionalEvent event) {
        for (IntegrationProperties.TransactionRule rule : properties.getRules()) {
            if (!rule.getEventType().equalsIgnoreCase(event.eventType())) {
                continue;
            }
            if (event.amount().compareTo(rule.getMinAmount()) < 0) {
                continue;
            }
            Operation operation = Operation.valueOf(rule.getOperation().toUpperCase());
            BigDecimal points = computePoints(rule, event.amount());
            if (points.signum() <= 0 && operation != Operation.PROCESS) {
                continue;
            }
            return Optional.of(new RuleDecision(operation, rule.getPointCurrency(), points,
                rule.getEventType(), rule.getFormula(), rule.getProcessType()));
        }
        return Optional.empty();
    }

    private BigDecimal computePoints(IntegrationProperties.TransactionRule rule, BigDecimal amount) {
        String formula = rule.getFormula().trim().toUpperCase();
        if ("AMOUNT".equals(formula)) {
            return amount.setScale(18, RoundingMode.HALF_UP);
        }
        if (formula.startsWith("FIXED:")) {
            return new BigDecimal(formula.substring("FIXED:".length())).setScale(18, RoundingMode.HALF_UP);
        }
        if (formula.startsWith("RATE:")) {
            BigDecimal rate = new BigDecimal(formula.substring("RATE:".length()));
            return amount.multiply(rate).setScale(18, RoundingMode.HALF_UP);
        }
        throw new IllegalArgumentException("Unsupported formula: " + rule.getFormula());
    }

    public record RuleDecision(
        Operation operation,
        String pointCurrency,
        BigDecimal points,
        String matchedRule,
        String formula,
        String processType
    ) {}
}
