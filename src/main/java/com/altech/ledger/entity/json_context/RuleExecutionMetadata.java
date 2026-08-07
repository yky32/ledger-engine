package com.altech.ledger.entity.json_context;

import java.util.List;

/**
 * JSON shape for {@code RuleExecution.metadata}: ordered list of rule ids.
 */
public record RuleExecutionMetadata(
    List<Detail> rules
) {
    /** One rule reference with sequence order. */
    public record Detail(String id, Integer seq) {}
}
