package com.altech.ledger.entity.dto.ingest;

/**
 * One Brain rule attempt during evaluation (Trust pack B).
 * {@code failStep} null when matched.
 */
public record EligibilityTraceEntry(
    String ruleCode,
    Integer priority,
    boolean matched,
    String failStep,
    String detail
) {}
