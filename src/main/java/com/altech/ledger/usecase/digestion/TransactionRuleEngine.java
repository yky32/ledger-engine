package com.altech.ledger.usecase.digestion;

import com.altech.ledger.entity.dto.ingest.EligibilityTraceEntry;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.usecase.factor.FactorMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Matches inbound events to runtime {@link DigestionRule} rows (DB only).
 * Trust pack B: builds {@link EligibilityTraceEntry} for each candidate (same eventType).
 * <p>
 * Eligibility = legacy columns compiled to factors + explicit {@code whenFactors} via {@link FactorMatcher}.
 */
@Component
@RequiredArgsConstructor
public class TransactionRuleEngine {
    public enum Operation { EARN, BURN, PROCESS }

    private final DigestionRuleRepository digestionRuleRepository;
    private final FactorMatcher factorMatcher;

    public EvaluationOutcome evaluate(TransactionalEvent event) {
        String lastReasonCode = "NO_RULE";
        String lastReason = "No matching digestion rule";
        List<EligibilityTraceEntry> trace = new ArrayList<>();

        List<DigestionRule> rules = digestionRuleRepository.findAllEnabledOrdered();
        for (DigestionRule rule : rules) {
            if (rule.getEventType() == null
                || event.eventType() == null
                || !rule.getEventType().equalsIgnoreCase(event.eventType())) {
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

            Object whenSpec = factorMatcher.effectiveWhenFactors(rule);
            FactorMatcher.MatchResult mr = factorMatcher.matchAll(event, whenSpec);
            if (!mr.matched()) {
                lastReasonCode = mr.failStep() == null ? "FACTOR" : mr.failStep();
                lastReason = mr.detail() == null ? "whenFactors not matched" : mr.detail();
                String path = mr.pathJoined();
                if (path != null) {
                    lastReason = lastReason + " · path=" + path;
                }
                trace.add(new EligibilityTraceEntry(
                    code, pri, false, lastReasonCode, lastReason, mr.path()));
                continue;
            }

            BigDecimal points;
            try {
                points = DigestionFormula.compute(formula, event.amount(), event.metadata());
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

            String pathDetail = "points=" + points.toPlainString();
            if (mr.pathJoined() != null) {
                pathDetail = pathDetail + " · path=" + mr.pathJoined();
            }
            trace.add(new EligibilityTraceEntry(
                code, pri, true, null, pathDetail, mr.path()));
            RuleDecision decision = new RuleDecision(
                operation,
                rule.getResultCurrency(),
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

    public record RuleDecision(
        Operation operation,
        String resultCurrency,
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
            return DigestionFormulaConfig.compute(formula, amount, null);
        }

        public static BigDecimal compute(Object formula, BigDecimal amount, Map<String, String> metadata) {
            return DigestionFormulaConfig.compute(formula, amount, metadata);
        }

        public static BigDecimal compute(String formula, BigDecimal amount) {
            return DigestionFormulaConfig.compute(formula, amount, null);
        }
    }
}
