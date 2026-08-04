package com.altech.ledger.onboarding;

import com.altech.ledger.api.LedgerDtos.AccountResponse;

import java.util.List;

public record BatchOnboardWalletResponse(
    int requested,
    int created,
    int alreadyExists,
    List<AccountResponse> createdWallets,
    List<String> alreadyExistingUserIds
) {}
