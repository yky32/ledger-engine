package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Partial update — null keeps existing. */
public record UpdateIngestPolicyRequestDto(
    Boolean isEnabled,
    Boolean isAutoCreateWallet,
    @Size(max = 16) String autoWalletSettlementCurrency,
    @Size(max = 16) String autoWalletEnsureCurrency,
    @Size(max = 50) String autoWalletAssociatedFrom,
    @Size(max = 50) String autoWalletNamePrefix,
    /** Product-stream COA for lazy onboard; blank clears to DEFAULT behaviour. */
    @Size(max = 40) String autoWalletCoaProfileCode,
    /**
     * Entry factors — empty list clears. Null = leave unchanged.
     * All must match or event is NOT ENTERED (skipped before Brain).
     */
    List<Map<String, Object>> entryFactors
) {}
