package com.altech.ledger.entity.json_context.transfer;

/** transfer TargetMetadata. */
public record TargetMetadata(
    String name,
    String accountId,
    String walletId,
    String bankName,
    String country
) {}
