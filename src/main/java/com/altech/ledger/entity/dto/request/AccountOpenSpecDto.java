package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.WalletAccountRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One account to open under the wallet.
 * <p>
 * Minimal indication for prod: only {@code role} is required.
 * Optional {@code productCode} overrides the default suffix (e.g. custom card id).
 */
public record AccountOpenSpecDto(
    @NotNull WalletAccountRole role,
    @Size(max = 32) String productCode,
    Boolean allowNegative
) {
    public AccountOpenSpecDto {
        if (allowNegative == null) {
            allowNegative = Boolean.FALSE;
        }
        if (productCode != null) {
            productCode = productCode.trim();
            if (productCode.isEmpty()) {
                productCode = null;
            }
        }
    }

    public static AccountOpenSpecDto of(WalletAccountRole role) {
        return new AccountOpenSpecDto(role, null, false);
    }

    public String resolvedRefCode() {
        if (productCode != null && !productCode.isBlank()) {
            return productCode;
        }
        return role.getRefCode();
    }
}
