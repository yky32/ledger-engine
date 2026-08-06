package com.altech.ledger.entity.json_context.ledger_movement;

/** Port of the-wallet-ledger LedgerMovementRecipientMetadata. */
public record LedgerMovementRecipientMetadata(
    String name,
    String identifier,
    String transferChannel,
    Boolean isSave
) {}
