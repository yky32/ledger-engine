package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

/** Bind all HOUSE_* COA rows to one company wallet and open accounts. */
public record AssignHouseWalletRequestDto(
    /** Default HOUSE (UAF company wallet). PROGRAM is accepted and renamed. */
    @Size(max = 100) String ownerId
) {}
