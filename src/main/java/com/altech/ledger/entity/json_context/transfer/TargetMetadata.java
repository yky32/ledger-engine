package com.altech.ledger.entity.json_context.transfer;

/**
 * Transfer target (to side): party, wallet/account, optional bank destination.
 */
public record TargetMetadata(
    String name,
    String accountId,
    String walletId,
    String bankName,
    String country
) {}
