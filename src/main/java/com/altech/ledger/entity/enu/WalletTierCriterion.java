package com.altech.ledger.entity.enu;

/**
 * How to measure a wallet for banding.
 */
public enum WalletTierCriterion {
    /** Sum of {@code account.ledgerBalance} for this wallet in the policy currency. */
    LEDGER_BALANCE;

    public static WalletTierCriterion from(String raw) {
        if (raw == null || raw.isBlank()) {
            return LEDGER_BALANCE;
        }
        return switch (raw.trim().toUpperCase()) {
            case "LEDGER_BALANCE", "AMOUNT_TOTAL", "BALANCE" -> LEDGER_BALANCE;
            default -> throw new IllegalArgumentException("Unsupported wallet tier criterion: " + raw);
        };
    }
}
