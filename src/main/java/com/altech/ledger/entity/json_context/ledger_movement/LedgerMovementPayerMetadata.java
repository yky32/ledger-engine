package com.altech.ledger.entity.json_context.ledger_movement;

/**
 * Payer side of a movement (name / identifier / type) for JSON context storage.
 */
public record LedgerMovementPayerMetadata(
    String name,
    String identifier,
    String type
) {}
