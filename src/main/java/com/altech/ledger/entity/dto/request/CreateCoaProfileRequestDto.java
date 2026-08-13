package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCoaProfileRequestDto(
    @NotBlank @Size(max = 40) String code,
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
