package com.altech.ledger.entity.json_context.transfer;

/**
 * Transfer originator (from side): party name and wallet/account references.
 */
public record OriginatorMetadata(
    String name,
    String accountId,
    String walletId,
    String country
) {}
