package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.LedgerMovementStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Patch movement status (e.g. force SETTLED / cancel-style transitions).
 */
public record UpdateLedgerMovementStatusRequestDto(
    @NotNull LedgerMovementStatus status,
    @Size(max = 500) String remarks
) {}
