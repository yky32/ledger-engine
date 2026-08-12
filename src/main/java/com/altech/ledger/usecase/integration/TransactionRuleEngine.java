package com.altech.ledger.usecase.integration;

import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.entity.po.integration.DigestionRule;
import com.altech.ledger.repository.DigestionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Matches inbound events to <b>runtime</b> {@link DigestionRule} rows (DB).
 * YAML only seeds the table at startup when empty.
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

            String formula = rule.getFormula() == null ? "" : rule.getFormula().trim();
            if (operation == Operation.EARN || operation == Operation.BURN) {
                if (isSpendBased(formula) && (event.amount() == null || event.amount().signum() <= 0)) {
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

    private static boolean isSpendBased(String formula) {
        if (formula == null || formula.isBlank()) {
            return true;
        }
        String f = formula.trim();
        if (f.startsWith("{")) {
            return true; // JSON rate/fixed uses amount
        }
        String u = f.toUpperCase(Locale.ROOT);
        if ("AMOUNT".equals(u) || u.startsWith("RATE:") || u.startsWith("MUL_ADD:")) {
            return true;
        }
        return u.startsWith("FIXED:") ? false : true;
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

    /** Scoring helpers for digestion formulas. */
    public static final class DigestionFormula {
        private DigestionFormula() {}

        public static BigDecimal compute(String formula, BigDecimal amount) {
            if (formula == null || formula.isBlank()) {
                throw new IllegalArgumentException("formula is blank");
            }
            String raw = formula.trim();
            BigDecimal amt = amount == null ? BigDecimal.ZERO : amount;

            // JSON: {"rate":0.01,"fixed":0}
            if (raw.startsWith("{")) {
                BigDecimal rate = extractJsonNumber(raw, "rate");
                BigDecimal fixed = extractJsonNumber(raw, "fixed");
                if (rate == null) {
                    rate = BigDecimal.ZERO;
                }
                if (fixed == null) {
                    fixed = BigDecimal.ZERO;
                }
                return amt.multiply(rate).add(fixed).setScale(18, RoundingMode.HALF_UP);
            }

            String f = raw.toUpperCase(Locale.ROOT);
            if ("AMOUNT".equals(f)) {
                return amt.setScale(18, RoundingMode.HALF_UP);
            }
            if (f.startsWith("FIXED:")) {
                return new BigDecimal(raw.substring("FIXED:".length()).trim()).setScale(18, RoundingMode.HALF_UP);
            }
            if (f.startsWith("RATE:")) {
                BigDecimal rate = new BigDecimal(raw.substring("RATE:".length()).trim());
                return amt.multiply(rate).setScale(18, RoundingMode.HALF_UP);
            }
            // MUL_ADD:rate:fixed → amount * rate + fixed
            if (f.startsWith("MUL_ADD:")) {
                String rest = raw.substring("MUL_ADD:".length()).trim();
                String[] parts = rest.split(":");
                if (parts.length < 1) {
                    throw new IllegalArgumentException("MUL_ADD needs rate");
                }
                BigDecimal rate = new BigDecimal(parts[0].trim());
                BigDecimal fixed = parts.length > 1 ? new BigDecimal(parts[1].trim()) : BigDecimal.ZERO;
                return amt.multiply(rate).add(fixed).setScale(18, RoundingMode.HALF_UP);
            }
            throw new IllegalArgumentException("Unsupported formula: " + formula);
        }

        private static BigDecimal extractJsonNumber(String json, String key) {
            // minimal parse — avoid extra deps
            String pattern = "\"" + key + "\"";
            int i = json.indexOf(pattern);
            if (i < 0) {
                return null;
            }
            int colon = json.indexOf(':', i + pattern.length());
            if (colon < 0) {
                return null;
            }
            int j = colon + 1;
            while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                j++;
            }
            int k = j;
            while (k < json.length()) {
                char c = json.charAt(k);
                if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                    break;
                }
                k++;
            }
            String num = json.substring(j, k).trim();
            if (num.isEmpty()) {
                return null;
            }
            return new BigDecimal(num);
        }
    }
}
