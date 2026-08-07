package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Create hierarchical COA account; blank segments get product defaults.
 */
public record CreateLedgerAccountRequestDto(
    String entity,
    String type,
    String subType,
    String buffer,
    String mainAccount,
    String subAccount,
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
    Boolean allowNegative
) {
    public CreateLedgerAccountRequestDto {
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
        if (allowNegative == null) {
            allowNegative = Boolean.FALSE;
        }
    }
}
