package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;

import jakarta.validation.constraints.NotNull;


/**
 * Create hierarchical COA account; blank segments get product defaults.
 */
public record CreateLedgerAccountRequestDto(
    String entity,
    String type,
    String subType,
    String buffer,
    String mainAccount,
    @NotNull Currency currency,
    Boolean allowNegative,
    Long walletId
) {
    public CreateLedgerAccountRequestDto {
        if (allowNegative == null) {
            allowNegative = Boolean.FALSE;
        }
    }
}
