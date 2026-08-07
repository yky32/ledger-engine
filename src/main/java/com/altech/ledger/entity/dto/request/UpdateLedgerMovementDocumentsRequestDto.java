package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Attach document metadata / remarks without changing balances.
 */
public record UpdateLedgerMovementDocumentsRequestDto(
    String files,
    @Size(max = 500) String remarks
) {}
