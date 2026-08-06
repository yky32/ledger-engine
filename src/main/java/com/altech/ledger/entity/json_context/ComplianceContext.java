package com.altech.ledger.entity.json_context;

/** Port of the-wallet-ledger ComplianceContext. */
public record ComplianceContext(
    String status,
    String screeningResult,
    String notes,
    String reviewedBy
) {}
