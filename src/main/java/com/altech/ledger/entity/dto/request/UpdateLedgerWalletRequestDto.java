package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.WalletStatus;

/** Partial update — status / primary account / display name. */
public record UpdateLedgerWalletRequestDto(
    WalletStatus status,
    Long accountId,
    String name
) {}
