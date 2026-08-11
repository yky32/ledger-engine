package com.altech.ledger.usecase.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Matches inbound events to integration rules and applies earn eligibility gates.
 */
@Component
@RequiredArgsConstructor
public class TransactionRuleEngine {
    public enum Operation { EARN, BURN, PROCESS }

    private final IntegrationProperties integrationProperties;

    /**
     * @return match decision, or empty with structured skip reason on the outcome
     */
    public EvaluationOutcome evaluate(TransactionalEvent event) {
        String lastReasonCode = "NO_RULE";
        String lastReason = "No matching rule";

        for (IntegrationProperties.TransactionRule rule : integrationProperties.getRules()) {
            if (rule.getEventType() == null
                || !rule.getEventType().equalsIgnoreCase(event.eventType())) {
                continue;
            }

            Operation operation;
            try {
                operation = Operation.valueOf(rule.getOperation().trim().toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                lastReasonCode = "BAD_RULE";
                lastReason = "Invalid operation on rule: " + rule.getOperation();
                continue;
            }

            // amount > 0 for spend-based formulas (AMOUNT / RATE); FIXED may use 0
            if (operation == Operation.EARN || operation == Operation.BURN) {
                String formula = rule.getFormula() == null ? "" : rule.getFormula().trim().toUpperCase(Locale.ROOT);
                boolean spendBased = formula.isEmpty() || formula.equals("AMOUNT") || formula.startsWith("RATE:");
                if (spendBased && (event.amount() == null || event.amount().signum() <= 0)) {
                    lastReasonCode = "AMOUNT";
                    lastReason = "amount must be > 0 for formula " + formula;
                    continue;
                }
            }

            BigDecimal min = rule.getMinAmount() == null ? BigDecimal.ZERO : rule.getMinAmount();
            if (event.amount().compareTo(min) < 0) {
                lastReasonCode = "MIN_AMOUNT";
                lastReason = "amount below minAmount " + min;
                continue;
            }

            if (rule.getEligibleCurrencies() != null && !rule.getEligibleCurrencies().isEmpty()) {
                Set<String> allowed = new HashSet<>();
                for (String c : rule.getEligibleCurrencies()) {
                    if (c != null && !c.isBlank()) {
                        allowed.add(c.trim().toUpperCase(Locale.ROOT));
                    }
                }
                String ccy = event.currency() == null ? null : event.currency().getIsoCode().toUpperCase(Locale.ROOT);
                if (ccy == null || !allowed.contains(ccy)) {
                    lastReasonCode = "CURRENCY";
                    lastReason = "currency " + ccy + " not in eligible list " + allowed;
                    continue;
                }
            }

            if (rule.getMaxAgeDays() != null) {
                if (event.occurredAt() == null) {
                    lastReasonCode = "AGE";
                    lastReason = "occurredAt required when maxAgeDays=" + rule.getMaxAgeDays();
                    continue;
                }
                long maxSeconds = rule.getMaxAgeDays().longValue() * 24L * 3600L;
                long ageSeconds = Duration.between(event.occurredAt(), Instant.now()).getSeconds();
                if (ageSeconds < 0) {
                    // future-dated: treat as age 0 OK, or skip? allow small clock skew — if future > 1 day skip
                    if (ageSeconds < -86400) {
                        lastReasonCode = "AGE";
                        lastReason = "occurredAt is too far in the future";
                        continue;
                    }
                } else if (ageSeconds > maxSeconds) {
                    lastReasonCode = "AGE";
                    lastReason = "occurredAt older than " + rule.getMaxAgeDays() + " days";
                    continue;
                }
            }

            BigDecimal points;
            try {
                points = computePoints(rule, event.amount());
            } catch (RuntimeException ex) {
                lastReasonCode = "FORMULA";
                lastReason = ex.getMessage() == null ? "formula error" : ex.getMessage();
                continue;
            }
            if (points.signum() <= 0 && operation != Operation.PROCESS) {
                lastReasonCode = "POINTS";
                lastReason = "computed points <= 0";
                continue;
            }

            return EvaluationOutcome.match(new RuleDecision(
                operation,
                rule.getPointCurrency(),
                points,
                rule.getEventType(),
                rule.getFormula(),
                rule.getProcessType()
            ));
        }

        return EvaluationOutcome.noMatch(lastReasonCode, lastReason);
    }

    private BigDecimal computePoints(IntegrationProperties.TransactionRule rule, BigDecimal amount) {
        String formula = rule.getFormula().trim().toUpperCase(Locale.ROOT);
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

    public record EvaluationOutcome(
        Optional<RuleDecision> decision,
        String skipReasonCode,
        String skipReason
    ) {
        public static EvaluationOutcome match(RuleDecision d) {
            return new EvaluationOutcome(Optional.of(d), null, null);
        }

        public static EvaluationOutcome noMatch(String code, String reason) {
            return new EvaluationOutcome(Optional.empty(), code, reason);
        }

        public boolean matched() {
            return decision != null && decision.isPresent();
        }
    }
}
