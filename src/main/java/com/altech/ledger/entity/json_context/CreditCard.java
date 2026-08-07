package com.altech.ledger.entity.json_context;

/** CreditCard json context. */
public record CreditCard(
    String brand,
    String last4,
    String expMonth,
    String expYear,
    String holderName,
    String token
) {}
