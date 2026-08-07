package com.altech.ledger.entity.enu;

/**
 * OrderType. extended with engine loyalty / transfer types.
 */
public enum OrderType {
    PAYMENT_LINK,
    WITHDRAWAL,
    WALLET_TRANSFER,
    DEPOSIT,
    ADJUSTMENT,
    ADJUSTMENT_REFUND,
    ADJUSTMENT_TOTAL,
    BANK_CHARGE,
    HANDLING_CHARGE,
    // engine extensions (new on top)
    IN_WALLET_TRANSFER,
    SWIFT_TRANSFER,
    EARN,
    BURN,
    PROCESS,
    CHARGE
}
