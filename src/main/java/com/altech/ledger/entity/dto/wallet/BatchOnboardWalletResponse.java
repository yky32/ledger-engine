package com.altech.ledger.entity.dto.wallet;

import java.util.List;

public record BatchOnboardWalletResponse(
    int requested,
    int created,
    int alreadyExists,
    List<WalletOnboardResponse> createdWallets,
    List<String> alreadyExistingUserIds
) {}
