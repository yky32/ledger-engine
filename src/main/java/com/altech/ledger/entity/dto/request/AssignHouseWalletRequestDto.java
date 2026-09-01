package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

/** Bind all HOUSE_* COA rows to one company wallet and open accounts. */
public record AssignHouseWalletRequestDto(
    /** Default PROGRAM. */
    @Size(max = 100) String ownerId
) {}
