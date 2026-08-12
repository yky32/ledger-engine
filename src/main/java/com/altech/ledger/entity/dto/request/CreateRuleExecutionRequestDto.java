package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRuleExecutionRequestDto(
    @NotBlank String name,
    String description,
    @NotNull OrderType orderType,
    Object metadata
) {}
