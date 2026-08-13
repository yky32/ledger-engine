package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

/** Partial update — null keeps existing. */
public record UpdateCoaProfileRequestDto(
    @Size(max = 200) String name,
    Boolean isDefault,
    Boolean isEnabled,
    @Size(max = 8) String entity,
    @Size(max = 8) String type,
    @Size(max = 8) String subType,
    @Size(max = 8) String buffer,
    @Size(max = 16) String lpCurrency,
    Boolean poolAllowNegative
) {}
