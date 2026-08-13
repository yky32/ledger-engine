package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create digestion rule. {@code formula} is JSON config, e.g.
 * {@code {"type":"RATE","rate":0.01}} — legacy strings still accepted and normalized.
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
    Integer maxAgeDays,
    @Size(max = 16) String pointCurrency,
    @NotNull Object formula,
    @Size(max = 40) String processType
) {}
