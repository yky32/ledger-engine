package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

/**
 * One account to open under the wallet (caller / SDK defined).
 * <p>
 * Multi-tenant product: ledger core does <strong>not</strong> own any client’s product-line
 * catalog. Integrators pass free-form {@link #refCode} values via SDK.
 * Primary is the base wallet ref ({@code primary=true} or blank {@code refCode}).
 */
public record AccountOpenSpecDto(
    /**
     * Opaque account suffix under the wallet base ref (client-defined).
     * Blank → primary (base ref only).
     */
    @Size(max = 64) String refCode,

    /** Optional display name for this account line. */
    @Size(max = 200) String name,

    /** When true (or refCode blank), this is the primary account (wallet.accountId). */
    Boolean primary,

    Boolean allowNegative
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
        // blank refCode implies primary line
        if (refCode == null) {
            primary = Boolean.TRUE;
        }
    }

    public static AccountOpenSpecDto primaryLine() {
        return new AccountOpenSpecDto(null, null, true, false);
    }

    public static AccountOpenSpecDto of(String refCode) {
        return new AccountOpenSpecDto(refCode, null, false, false);
    }

    public boolean isPrimaryLine() {
        return Boolean.TRUE.equals(primary) || refCode == null;
    }

    /** Label used in account name when caller did not pass name. */
    public String label() {
        if (name != null) {
            return name;
        }
        if (isPrimaryLine()) {
            return "PRIMARY";
        }
        return refCode;
    }
}
