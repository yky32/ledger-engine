package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Product onboarding: <b>one wallet per ownerId</b> + account lines.
 */
public record CreateWalletOnboardRequestDto(
    @NotBlank @Size(max = 100) String ownerId,
    @NotNull Currency settlementCurrency,
    @Size(max = 200) String name,
    @Size(max = 32) List<@Valid AccountOpenSpecDto> accounts
) {
    public CreateWalletOnboardRequestDto {
        if (ownerId != null) {
            ownerId = ownerId.trim();
        }
        if (name != null) {
            name = name.trim();
        }
    }

    public CreateWalletOnboardRequestDto(String ownerId, Currency settlementCurrency, String name) {
        this(ownerId, settlementCurrency, name, null);
    }
}
