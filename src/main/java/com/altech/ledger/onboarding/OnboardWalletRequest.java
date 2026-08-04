package com.altech.ledger.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OnboardWalletRequest(
    @NotBlank @Size(max = 100) String userId,
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
    @Size(max = 200) String name
) {}
