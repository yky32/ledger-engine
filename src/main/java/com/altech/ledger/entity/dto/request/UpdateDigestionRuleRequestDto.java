package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Partial update — null fields keep existing values. */
public record UpdateDigestionRuleRequestDto(
    @Size(max = 200) String name,
    @Size(max = 80) String eventType,
    @Size(max = 20) String operation,
    Boolean isEnabled,
    Integer priority,
    BigDecimal minAmount,
    List<@Size(max = 16) String> eligibleCurrencies,
    List<@Size(max = 16) String> eligibleMccs,
    Integer maxAgeDays,
    @Size(max = 16) String pointCurrency,
    Object formula,
    @Size(max = 40) String processType,
    /** Pass empty list to clear whenFactors. */
    List<Map<String, Object>> whenFactors
) {}
