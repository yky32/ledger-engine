package com.altech.ledger.entity.json_context.transfer;

/** Port of the-wallet-ledger transfer OriginatorMetadata. */
public record OriginatorMetadata(
    String name,
    String accountId,
    String walletId,
    String country
) {}
