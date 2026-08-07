package com.altech.ledger.entity.json_context;

/**
 * Bank account details stored as JSON context (e.g. deposit source / payout destination).
 */
public record Bank(
    String bankName,
    String bankCode,
    String accountNumber,
    String accountName,
    String swiftCode,
    String country
) {}
