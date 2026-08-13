package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

/** Partial update — null keeps existing. */
public record UpdateCoaProfileRequestDto(
    @Size(max = 200) String name,
    Boolean isDefault,
    Boolean isEnabled,
    Object bindings
) {}
