package com.altech.ledger.entity.json_context;

/** Port of the-wallet-ledger Bank json context. */
public record Bank(
    String bankName,
    String bankCode,
    String accountNumber,
    String accountName,
    String swiftCode,
    String country
) {}
