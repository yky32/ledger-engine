package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Product onboarding: create one wallet for a user + currency (optional CRM external ids).
 */
public record CreateWalletOnboardRequestDto(
    @NotBlank @Size(max = 100) String userId,
    @NotNull Currency currency,
    @Size(max = 200) String name,
    @Size(max = 100) String externalId,
    @Size(max = 50) String externalType
) {
    public CreateWalletOnboardRequestDto {
        if (userId != null) {
            userId = userId.trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (externalId != null) {
            externalId = externalId.trim();
        }
        if (externalType != null) {
            externalType = externalType.trim();
        }
    }
}
