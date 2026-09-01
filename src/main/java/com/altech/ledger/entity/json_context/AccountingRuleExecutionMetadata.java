package com.altech.ledger.entity.json_context;

import java.util.List;

/**
 * JSON shape for {@code AccountingRuleExecution.metadata}: ordered list of account-rule ids.
 */
public record AccountingRuleExecutionMetadata(
    List<Detail> rules
) {
    /** One rule reference with sequence order. */
    public record Detail(String id, Integer seq) {}
}
