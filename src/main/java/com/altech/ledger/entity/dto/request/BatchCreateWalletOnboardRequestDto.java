package com.altech.ledger.entity.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** CRM / legacy bulk wallet onboarding. */
public record BatchCreateWalletOnboardRequestDto(
    @NotEmpty @Size(max = 1000) List<@Valid CreateWalletOnboardRequestDto> wallets
) {}
