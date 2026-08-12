package com.altech.ledger.entity.dto.request;

/** Attach / update movement documents / free-form contexts. */
public record UpdateLedgerMovementDocumentsRequestDto(
    Object files,
    String remarks,
    Object complianceContext,
    Object metadata
) {}
