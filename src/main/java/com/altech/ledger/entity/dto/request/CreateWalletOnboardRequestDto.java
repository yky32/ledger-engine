package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Product onboarding: <b>one wallet per ownerId</b> + account lines.
 */
public record CreateWalletOnboardRequestDto(
    @NotBlank @Size(max = 100) String ownerId,
    @NotNull Currency settlementCurrency,
    @Size(max = 200) String name,
    /** Optional vanity / premium display code; if blank, generator placeholder runs (may leave null). */
    @Size(max = 64) String vanityCode,
    @Size(max = 32) List<@Valid AccountOpenSpecDto> accounts
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
    }

    public CreateWalletOnboardRequestDto(String ownerId, Currency settlementCurrency, String name) {
        this(ownerId, settlementCurrency, name, null, null);
    }

    public CreateWalletOnboardRequestDto(
        String ownerId,
        Currency settlementCurrency,
        String name,
        List<AccountOpenSpecDto> accounts
    ) {
        this(ownerId, settlementCurrency, name, null, accounts);
    }
}
