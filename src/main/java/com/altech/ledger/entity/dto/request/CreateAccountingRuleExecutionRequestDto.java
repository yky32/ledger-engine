package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateAccountingRuleExecutionRequestDto(
    @NotBlank String name,
    String description,
    @NotNull OrderType orderType,
    String eventType,
    List<AccountingRuleRefDto> rules,
    String metadata
) {}
