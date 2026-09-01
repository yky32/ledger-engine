package com.altech.ledger.entity.dto.request;

/**
 * One leg in an {@code AccountingRuleExecution} walk.
 * {@code id} is {@code AccountingRule.id}.
 */
public record AccountingRuleRefDto(
    String id,
    Integer seq
) {}
