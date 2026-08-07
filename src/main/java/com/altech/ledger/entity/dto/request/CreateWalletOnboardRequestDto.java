package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Phase-1 single wallet onboarding (TGT: {@code Create*RequestDto}).
 */
public record CreateWalletOnboardRequestDto(
    @NotBlank @Size(max = 100) String userId,
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
    @Size(max = 200) String name,
    @Size(max = 100) String externalId,
    @Size(max = 50) String externalType
) {
    public CreateWalletOnboardRequestDto {
        if (userId != null) {
            userId = userId.trim();
        }
        if (currency != null) {
            currency = currency.trim().toUpperCase();
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
