package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

/** Partial update — null keeps existing. */
public record UpdateIngestPolicyRequestDto(
    Boolean isEnabled,
    Boolean isAutoCreateWallet,
    @Size(max = 16) String autoWalletSettlementCurrency,
    @Size(max = 16) String autoWalletEnsureCurrency,
    @Size(max = 50) String autoWalletAssociatedFrom,
    @Size(max = 50) String autoWalletNamePrefix,
    /** Product-stream COA for lazy onboard; blank clears to DEFAULT behaviour. */
    @Size(max = 40) String autoWalletCoaProfileCode
) {}
