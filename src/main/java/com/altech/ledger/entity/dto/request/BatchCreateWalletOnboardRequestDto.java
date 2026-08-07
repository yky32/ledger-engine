package com.altech.ledger.entity.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * CRM / bulk wallet onboarding: list of single-wallet create requests (soft-idempotent).
 */
public record BatchCreateWalletOnboardRequestDto(
    @NotEmpty @Size(max = 1000) List<@Valid CreateWalletOnboardRequestDto> wallets
) {}
