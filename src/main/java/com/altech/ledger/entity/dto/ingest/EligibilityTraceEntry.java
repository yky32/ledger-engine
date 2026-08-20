package com.altech.ledger.entity.dto.ingest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * One Brain / Door rule attempt (Trust pack B + Factor explain).
 * {@code failStep} null when matched.
 * {@code matchedPath} e.g. {@code ["G12","currency:eq"]} when FactorSet explains a hit.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EligibilityTraceEntry(
    String ruleCode,
    Integer priority,
    boolean matched,
    String failStep,
    String detail,
    /** Explain path segments (group id / leaf id); null if N/A. */
    List<String> matchedPath
) {
    /** Compat 5-arg. */
    public EligibilityTraceEntry(
        String ruleCode,
        Integer priority,
        boolean matched,
        String failStep,
        String detail
    ) {
        this(ruleCode, priority, matched, failStep, detail, null);
    }
}
