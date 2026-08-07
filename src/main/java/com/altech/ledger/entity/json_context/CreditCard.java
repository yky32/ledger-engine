package com.altech.ledger.entity.json_context;

/**
 * Tokenized card snapshot for payment-method style metadata (no full PAN).
 */
public record CreditCard(
    String brand,
    String last4,
    String expMonth,
    String expYear,
    String holderName,
    String token
) {}
