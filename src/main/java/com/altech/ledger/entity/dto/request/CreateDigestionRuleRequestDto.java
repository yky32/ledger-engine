package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create digestion rule.
 * <p>
 * Eligibility filters + scoring formula live on the same row:
 * eventType, minAmount, eligibleCurrencies, eligibleMccs, maxAgeDays → then {@code formula}.
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
    /** MCC allow-list; empty/null = any. */
    List<@Size(max = 16) String> eligibleMccs,
    Integer maxAgeDays,
    @Size(max = 16) String pointCurrency,
    @NotNull Object formula,
    @Size(max = 40) String processType
) {}
