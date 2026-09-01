package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Product onboarding: <b>one wallet per ownerId</b> + account lines.
 * Optional {@code coaProfileCode} selects product-stream COA (e.g. custom profile code).
 */
public record CreateWalletOnboardRequestDto(
    @NotBlank @Size(max = 100) String ownerId,
    @NotNull Currency settlementCurrency,
    @Size(max = 200) String name,
    /** Optional vanity / premium display code; if blank, generator placeholder runs (may leave null). */
    @Size(max = 64) String vanityCode,
    /** Optional COA profile code; null/blank → default profile. */
    @Size(max = 40) String coaProfileCode,
    @Size(max = 32) List<@Valid AccountOpenSpecDto> accounts,
    /** Optional client main-account (e.g. 9089…). Blank → engine generates. */
    @Size(max = 32) String mainAccount
) {
    public CreateWalletOnboardRequestDto {
        if (ownerId != null) {
            ownerId = ownerId.trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (vanityCode != null) {
            vanityCode = vanityCode.trim();
            if (vanityCode.isEmpty()) {
                vanityCode = null;
            }
        }
        if (coaProfileCode != null) {
            coaProfileCode = coaProfileCode.trim();
            if (coaProfileCode.isEmpty()) {
                coaProfileCode = null;
            } else {
                coaProfileCode = coaProfileCode.toUpperCase(java.util.Locale.ROOT);
            }
        }
        if (mainAccount != null) {
            mainAccount = mainAccount.trim();
            if (mainAccount.isEmpty()) {
                mainAccount = null;
            }
        }
    }

    public CreateWalletOnboardRequestDto(String ownerId, Currency settlementCurrency, String name) {
        this(ownerId, settlementCurrency, name, null, null, null, null);
    }

    public CreateWalletOnboardRequestDto(
        String ownerId,
        Currency settlementCurrency,
        String name,
        List<AccountOpenSpecDto> accounts
    ) {
        this(ownerId, settlementCurrency, name, null, null, accounts, null);
    }

    public CreateWalletOnboardRequestDto(
        String ownerId,
        Currency settlementCurrency,
        String name,
        String coaProfileCode,
        List<AccountOpenSpecDto> accounts
    ) {
        this(ownerId, settlementCurrency, name, null, coaProfileCode, accounts, null);
    }

    public CreateWalletOnboardRequestDto(
        String ownerId,
        Currency settlementCurrency,
        String name,
        String vanityCode,
        String coaProfileCode,
        List<AccountOpenSpecDto> accounts
    ) {
        this(ownerId, settlementCurrency, name, vanityCode, coaProfileCode, accounts, null);
    }
}
