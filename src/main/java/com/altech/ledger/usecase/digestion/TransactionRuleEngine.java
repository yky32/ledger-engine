package com.altech.ledger.usecase.digestion;

import com.altech.ledger.entity.dto.ingest.EligibilityTraceEntry;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.repository.DigestionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Matches inbound events to runtime {@link DigestionRule} rows (DB only).
 * Trust pack B: builds {@link EligibilityTraceEntry} for each candidate (same eventType).
 */
@Component
@RequiredArgsConstructor
public class TransactionRuleEngine {
    public enum Operation { EARN, BURN, PROCESS }

    private final DigestionRuleRepository digestionRuleRepository;

    public EvaluationOutcome evaluate(TransactionalEvent event) {
        String lastReasonCode = "NO_RULE";
        String lastReason = "No matching digestion rule";
        List<EligibilityTraceEntry> trace = new ArrayList<>();

        List<DigestionRule> rules = digestionRuleRepository.findAllEnabledOrdered();
        for (DigestionRule rule : rules) {
            if (rule.getEventType() == null
                || event.eventType() == null
                || !rule.getEventType().equalsIgnoreCase(event.eventType())) {
                // different product event — not a candidate
                continue;
            }

            String code = rule.getCode() != null ? rule.getCode() : rule.getEventType();
            Integer pri = rule.getPriority();

            Operation operation;
            try {
                operation = Operation.valueOf(rule.getOperation().trim().toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                lastReasonCode = "BAD_RULE";
                lastReason = "Invalid operation on rule: " + rule.getOperation();
                trace.add(fail(code, pri, "BAD_RULE", lastReason));
                continue;
            }

            Object formula = rule.getFormula();
            if (operation == Operation.EARN || operation == Operation.BURN) {
                if (DigestionFormulaConfig.isSpendBased(formula)
                    && (event.amount() == null || event.amount().signum() <= 0)) {
                    lastReasonCode = "AMOUNT";
                    lastReason = "amount must be > 0 for formula " + formula;
                    trace.add(fail(code, pri, "AMOUNT", lastReason));
                    continue;
                }
            }

            BigDecimal min = rule.getMinAmount() == null ? BigDecimal.ZERO : rule.getMinAmount();
            if (event.amount() != null && event.amount().compareTo(min) < 0) {
                lastReasonCode = "MIN_AMOUNT";
                lastReason = "amount below minAmount " + min;
                trace.add(fail(code, pri, "MIN_AMOUNT", lastReason));
                continue;
            }

            List<String> eligible = DigestionRuleUseCase.splitCodes(rule.getEligibleCurrencies());
            if (!eligible.isEmpty()) {
                Set<String> allowed = new HashSet<>(eligible);
                String ccy = event.currency() == null ? null : event.currency().getIsoCode().toUpperCase(Locale.ROOT);
                if (ccy == null || !allowed.contains(ccy)) {
                    lastReasonCode = "CURRENCY";
                    lastReason = "currency " + ccy + " not in eligible list " + allowed;
                    trace.add(fail(code, pri, "CURRENCY", lastReason));
                    continue;
                }
            }

            List<String> mccs = DigestionRuleUseCase.splitCodes(rule.getEligibleMccs());
            if (!mccs.isEmpty()) {
                String eventMcc = extractMcc(event);
                Set<String> allowedMcc = new HashSet<>(mccs);
                if (eventMcc == null || eventMcc.isBlank() || !allowedMcc.contains(eventMcc)) {
                    lastReasonCode = "MCC";
                    lastReason = "mcc " + eventMcc + " not in eligible list " + allowedMcc
                        + " (send metadata.mcc / mccCode / merchantCategoryCode)";
                    trace.add(fail(code, pri, "MCC", lastReason));
                    continue;
                }
            }

            if (rule.getMaxAgeDays() != null) {
                if (event.occurredAt() == null) {
                    lastReasonCode = "AGE";
                    lastReason = "occurredAt required when maxAgeDays=" + rule.getMaxAgeDays();
                    trace.add(fail(code, pri, "AGE", lastReason));
                    continue;
                }
                long maxSeconds = rule.getMaxAgeDays().longValue() * 24L * 3600L;
                long ageSeconds = Duration.between(event.occurredAt(), Instant.now()).getSeconds();
                if (ageSeconds < -86400) {
                    lastReasonCode = "AGE";
                    lastReason = "occurredAt is too far in the future";
                    trace.add(fail(code, pri, "AGE", lastReason));
                    continue;
                } else if (ageSeconds > maxSeconds) {
                    lastReasonCode = "AGE";
                    lastReason = "occurredAt older than " + rule.getMaxAgeDays() + " days";
                    trace.add(fail(code, pri, "AGE", lastReason));
                    continue;
                }
            }

            BigDecimal points;
            try {
                points = DigestionFormula.compute(formula, event.amount());
            } catch (RuntimeException ex) {
                lastReasonCode = "FORMULA";
                lastReason = ex.getMessage() == null ? "formula error" : ex.getMessage();
                trace.add(fail(code, pri, "FORMULA", lastReason));
                continue;
            }
            if (points.signum() <= 0 && operation != Operation.PROCESS) {
                lastReasonCode = "POINTS";
                lastReason = "computed points <= 0";
                trace.add(fail(code, pri, "POINTS", lastReason));
                continue;
            }

            trace.add(new EligibilityTraceEntry(
                code, pri, true, null, "points=" + points.toPlainString()));
            RuleDecision decision = new RuleDecision(
                operation,
                rule.getPointCurrency(),
                points,
                code,
                formula,
                rule.getProcessType()
            );
            return EvaluationOutcome.match(decision, List.copyOf(trace));
        }

        return EvaluationOutcome.noMatch(lastReasonCode, lastReason, List.copyOf(trace));
    }

    private static EligibilityTraceEntry fail(String code, Integer pri, String step, String detail) {
        return new EligibilityTraceEntry(code, pri, false, step, detail);
    }

    static String extractMcc(TransactionalEvent event) {
        if (event == null || event.metadata() == null || event.metadata().isEmpty()) {
            return null;
        }
        for (String key : List.of("mcc", "mccCode", "merchantCategoryCode", "MCC")) {
            String v = event.metadata().get(key);
            if (v != null && !v.isBlank()) {
                return v.trim().toUpperCase(Locale.ROOT);
            }
        }
        return null;
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
        String skipReason,
        List<EligibilityTraceEntry> trace
    ) {
        public static EvaluationOutcome match(RuleDecision d, List<EligibilityTraceEntry> trace) {
            return new EvaluationOutcome(Optional.of(d), null, null,
                trace == null ? List.of() : List.copyOf(trace));
        }

        public static EvaluationOutcome noMatch(String code, String reason, List<EligibilityTraceEntry> trace) {
            return new EvaluationOutcome(Optional.empty(), code, reason,
                trace == null ? List.of() : List.copyOf(trace));
        }

        public boolean matched() {
            return decision != null && decision.isPresent();
        }

        public List<EligibilityTraceEntry> trace() {
            return trace == null ? List.of() : trace;
        }
    }

    public static final class DigestionFormula {
        private DigestionFormula() {}

        public static BigDecimal compute(Object formula, BigDecimal amount) {
            return DigestionFormulaConfig.compute(formula, amount);
        }

        public static BigDecimal compute(String formula, BigDecimal amount) {
            return DigestionFormulaConfig.compute(formula, amount);
        }
    }
}
