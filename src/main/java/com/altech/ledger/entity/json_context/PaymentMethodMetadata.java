package com.altech.ledger.entity.json_context;

/** Port of the-wallet-ledger PaymentMethodMetadata. */
public record PaymentMethodMetadata(
    Bank bank,
    CreditCard creditCard
) {}
