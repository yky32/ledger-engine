package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create digestion rule.
 * Legacy filters (minAmount, currencies, mccs, maxAgeDays) still accepted and compiled to factors.
 * Prefer {@code whenFactors} for rich ops (between, nin, gt, …).
 */
public record CreateDigestionRuleRequestDto(
    @NotBlank @Size(max = 80) String code,
    @Size(max = 200) String name,
    @NotBlank @Size(max = 80) String eventType,
    @Size(max = 20) String operation,
    Boolean isEnabled,
    Integer priority,
    BigDecimal minAmount,
    List<@Size(max = 16) String> eligibleCurrencies,
    List<@Size(max = 16) String> eligibleMccs,
    Integer maxAgeDays,
    @Size(max = 16) String pointCurrency,
    @NotNull Object formula,
    @Size(max = 40) String processType,
    /** Explicit when-factors: JSON array (AND) or FactorSet object. */
    Object whenFactors
) {}
