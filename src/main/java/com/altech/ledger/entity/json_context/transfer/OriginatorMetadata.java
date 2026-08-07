package com.altech.ledger.entity.json_context.transfer;

/** transfer OriginatorMetadata. */
public record OriginatorMetadata(
    String name,
    String accountId,
    String walletId,
    String country
) {}
