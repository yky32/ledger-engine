package com.altech.ledger.entity.enu;

/**
 * Account kinds that may be opened under a wallet (account-set).
 * <p>
 * Callers pass these on wallet create to indicate which product lines to open.
 * {@link #MAIN} is always ensured; others are optional (loan, cards, reward, …).
 * Codes are stable external-reference suffixes for prod bulk onboard.
 */
public enum WalletAccountRole {
    /** Primary balance bucket; wallet.accountId points here. */
    MAIN("MAIN"),
    /** Loan facility line. */
    LOAN("LOAN"),
    /** Credit card yellow (product code 88). */
    CC_YELLOW("88"),
    /** Credit card purple (product code 89). */
    CC_PURPLE("89"),
    /** Optional separate reward/points line (if distinct from MAIN). */
    REWARD("REWARD");

    private final String refCode;

    WalletAccountRole(String refCode) {
        this.refCode = refCode;
    }

    /** Suffix used in account fullNumber after the wallet base ref. */
    public String getRefCode() {
        return refCode;
    }

    public boolean isPrimary() {
        return this == MAIN;
    }
}
