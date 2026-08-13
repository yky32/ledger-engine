package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create COA profile. {@code bindings} optional — defaults to built-in 3 roles.
 */
public record CreateCoaProfileRequestDto(
    @NotBlank @Size(max = 40) String code,
    @Size(max = 200) String name,
    Boolean isDefault,
    Boolean isEnabled,
    Object bindings
) {}
