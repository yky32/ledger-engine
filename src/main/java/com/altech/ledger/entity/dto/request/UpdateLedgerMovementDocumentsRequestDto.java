package com.altech.ledger.entity.dto.request;

public record UpdateLedgerMovementDocumentsRequestDto(
    String files,
    String remarks,
    String complianceContext,
    String metadata
) {}
