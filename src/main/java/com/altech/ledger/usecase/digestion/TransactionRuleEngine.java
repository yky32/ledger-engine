package com.altech.ledger.usecase.digestion;

import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.repository.DigestionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Matches inbound events to runtime {@link DigestionRule} rows (DB only).
 */
@Component
@RequiredArgsConstructor
public class TransactionRuleEngine {
    public enum Operation { EARN, BURN, PROCESS }

    private final DigestionRuleRepository digestionRuleRepository;

    public EvaluationOutcome evaluate(TransactionalEvent event) {
        String lastReasonCode = "NO_RULE";
        String lastReason = "No matching digestion rule";

        List<DigestionRule> rules = digestionRuleRepository.findAllEnabledOrdered();
        for (DigestionRule rule : rules) {
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

            Object formula = rule.getFormula();
            if (operation == Operation.EARN || operation == Operation.BURN) {
                if (DigestionFormulaConfig.isSpendBased(formula)
                    && (event.amount() == null || event.amount().signum() <= 0)) {
                    lastReasonCode = "AMOUNT";
                    lastReason = "amount must be > 0 for formula " + formula;
                    continue;
                }
            }

            BigDecimal min = rule.getMinAmount() == null ? BigDecimal.ZERO : rule.getMinAmount();
            if (event.amount() != null && event.amount().compareTo(min) < 0) {
                lastReasonCode = "MIN_AMOUNT";
                lastReason = "amount below minAmount " + min;
                continue;
            }

            List<String> eligible = DigestionRuleUseCase.splitCurrencies(rule.getEligibleCurrencies());
            if (!eligible.isEmpty()) {
                Set<String> allowed = new HashSet<>(eligible);
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
                if (ageSeconds < -86400) {
                    lastReasonCode = "AGE";
                    lastReason = "occurredAt is too far in the future";
                    continue;
                } else if (ageSeconds > maxSeconds) {
                    lastReasonCode = "AGE";
                    lastReason = "occurredAt older than " + rule.getMaxAgeDays() + " days";
                    continue;
                }
            }

            BigDecimal points;
            try {
                points = DigestionFormula.compute(formula, event.amount());
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
                rule.getCode() != null ? rule.getCode() : rule.getEventType(),
                formula,
                rule.getProcessType()
            ));
        }

        return EvaluationOutcome.noMatch(lastReasonCode, lastReason);
    }

    public record RuleDecision(
        Operation operation,
        String pointCurrency,
        BigDecimal points,
        String matchedRule,
        Object formula,
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

    /**
     * Scoring facade — delegates to {@link DigestionFormulaConfig}.
     * Accepts JSON object / Map / legacy string DSL.
     */
    public static final class DigestionFormula {
        private DigestionFormula() {}

        public static BigDecimal compute(Object formula, BigDecimal amount) {
            return DigestionFormulaConfig.compute(formula, amount);
        }

        /** @deprecated prefer {@link #compute(Object, BigDecimal)} */
        public static BigDecimal compute(String formula, BigDecimal amount) {
            return DigestionFormulaConfig.compute(formula, amount);
        }
    }
}
