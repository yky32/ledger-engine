package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import jakarta.validation.constraints.Size;

/**
 * One account line under a wallet.
 * <p>
 * Primary line uses the wallet {@code settlementCurrency} (omit or {@code primary=true}).
 * Extra lines should set {@link #currency} (e.g. {@code LP}) under the same wallet main COA.
 */
public record AccountOpenSpecDto(
    /**
     * Optional product / leaf code (client-defined). Blank → primary line.
     */
    @Size(max = 64) String refCode,

    /** Optional display name for this account line. */
    @Size(max = 200) String name,

    /** When true (or refCode blank), this is the primary account (wallet.accountId). */
    Boolean primary,

    Boolean allowNegative,

    /**
     * Account currency. Primary defaults to wallet settlement currency.
     * Extra lines should set this (e.g. {@code LP}).
     */
    Currency currency
) {
    public AccountOpenSpecDto {
        if (allowNegative == null) {
            allowNegative = Boolean.FALSE;
        }
        if (primary == null) {
            primary = Boolean.FALSE;
        }
        if (refCode != null) {
            refCode = refCode.trim();
            if (refCode.isEmpty()) {
                refCode = null;
            }
        }
        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                name = null;
            }
        }
        if (refCode == null && currency == null) {
            primary = Boolean.TRUE;
        }
    }

    public static AccountOpenSpecDto primaryLine() {
        return new AccountOpenSpecDto(null, null, true, false, null);
    }

    public static AccountOpenSpecDto ofCurrency(Currency currency) {
        return new AccountOpenSpecDto(
            currency == null ? null : currency.getIsoCode(),
            currency == null ? null : currency.getIsoCode(),
            false,
            false,
            currency
        );
    }

    public boolean isPrimaryLine() {
        return Boolean.TRUE.equals(primary) || (refCode == null && currency == null);
    }

    public String label() {
        if (name != null) {
            return name;
        }
        if (isPrimaryLine()) {
            return "PRIMARY";
        }
        if (currency != null) {
            return currency.getIsoCode();
        }
        return refCode;
    }
}
