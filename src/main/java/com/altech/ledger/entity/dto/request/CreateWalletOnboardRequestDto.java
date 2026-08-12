package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Product onboarding: <b>one wallet per customer</b> + account lines under that wallet.
 * <p>
 * {@link #associatedIdentifier} — CRM customer id (stored as {@code ownerId}).<br>
 * {@link #settlementCurrency} — default settlement + primary account currency.<br>
 * {@link #accounts} — optional extra books (e.g. LP).
 */
public record CreateWalletOnboardRequestDto(
    @NotBlank @Size(max = 100) String associatedIdentifier,
    @NotNull Currency settlementCurrency,
    @Size(max = 200) String name,
    @Size(max = 32) List<@Valid AccountOpenSpecDto> accounts
) {
    public CreateWalletOnboardRequestDto {
        if (associatedIdentifier != null) {
            associatedIdentifier = associatedIdentifier.trim();
        }
        if (name != null) {
            name = name.trim();
        }
    }

    public CreateWalletOnboardRequestDto(String associatedIdentifier, Currency settlementCurrency, String name) {
        this(associatedIdentifier, settlementCurrency, name, null);
    }

    /** Compat: ignore associatedFrom (no longer stored on wallet). */
    public CreateWalletOnboardRequestDto(
        String associatedIdentifier,
        Currency settlementCurrency,
        String name,
        String associatedFrom,
        List<AccountOpenSpecDto> accounts
    ) {
        this(associatedIdentifier, settlementCurrency, name, accounts);
    }
}
