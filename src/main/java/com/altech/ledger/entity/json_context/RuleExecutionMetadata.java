package com.altech.ledger.entity.json_context;

import java.util.List;

/** RuleExecutionMetadata. */
public record RuleExecutionMetadata(
    List<Detail> rules
) {
    public record Detail(String id, Integer seq) {}
}
