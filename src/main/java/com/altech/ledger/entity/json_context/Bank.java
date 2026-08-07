package com.altech.ledger.entity.json_context;

/** Bank json context. */
public record Bank(
    String bankName,
    String bankCode,
    String accountNumber,
    String accountName,
    String swiftCode,
    String country
) {}
