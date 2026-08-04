package com.altech.ledger.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchOnboardWalletRequest(
    @NotEmpty @Size(max = 1000) List<@Valid OnboardWalletRequest> wallets
) {}
