package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.json_context.WalletTierBand;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Partial update — null keeps existing. {@code bands} replaces the full list when present. */
public record UpdateWalletTierPolicyRequestDto(
    Boolean isEnabled,
    String criterion,
    @Size(max = 16) String currency,
    List<WalletTierBand> bands
) {}
