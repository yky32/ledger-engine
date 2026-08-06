package com.altech.ledger.entity.json_context;

import java.util.List;

/** Port of the-wallet-ledger RuleExecutionMetadata. */
public record RuleExecutionMetadata(
    List<Detail> rules
) {
    public record Detail(String id, Integer seq) {}
}
