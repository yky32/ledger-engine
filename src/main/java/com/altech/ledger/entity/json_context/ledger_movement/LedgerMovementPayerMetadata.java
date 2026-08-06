package com.altech.ledger.entity.json_context.ledger_movement;

/** Port of the-wallet-ledger LedgerMovementPayerMetadata. */
public record LedgerMovementPayerMetadata(
    String name,
    String identifier,
    String type
) {}
