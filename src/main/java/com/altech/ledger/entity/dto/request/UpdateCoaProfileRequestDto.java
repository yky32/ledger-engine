package com.altech.ledger.entity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

/** Partial update — null keeps existing. Pass empty string to clear transactionCode. */
public record UpdateCoaProfileRequestDto(
    @Size(max = 200) String name,
    @Size(max = 64) String transactionCode,
    Boolean isDefault,
    Boolean isEnabled,
    @Size(max = 8) String entity,
    @Size(max = 8) String type,
    @Size(max = 8) String subType,
    @Size(max = 8) String buffer,
    @JsonAlias("lpCurrency") @Size(max = 16) String currency,
    Boolean poolAllowNegative
) {}
