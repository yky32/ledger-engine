package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.WalletStatus;

/** Partial update — status / primary account / display name / vanity. */
public record UpdateLedgerWalletRequestDto(
    WalletStatus status,
    Long accountId,
    String name,
    /** Set vanity display code; blank clears. Null = leave unchanged. */
    String vanityCode
) {}
