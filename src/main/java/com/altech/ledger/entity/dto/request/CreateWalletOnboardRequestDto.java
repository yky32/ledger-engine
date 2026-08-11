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
 * {@link #associatedIdentifier} — CRM / client customer id (unique wallet key).<br>
 * {@link #settlementCurrency} — wallet default settlement currency; primary account currency.<br>
 * {@link #accounts} — optional extra accounts (e.g. {@code LP}) under the same wallet.
 * Primary is always opened in {@link #settlementCurrency}; omit {@code accounts} for primary only.
 */
public record CreateWalletOnboardRequestDto(
    /** Associated party id (CRM cust id, member id, …). Sole identity for create. */
    @NotBlank @Size(max = 100) String associatedIdentifier,

    /** Default settlement currency (+ primary account currency). */
    @NotNull Currency settlementCurrency,

    @Size(max = 200) String name,

    /** System that owns associatedIdentifier (e.g. CRM, CORE_BANKING). Default CRM. */
    @Size(max = 50) String associatedFrom,

    /**
     * Extra account lines under this wallet. Primary (settlement currency) is always opened.
     * Example: {@code [{"currency":"LP","name":"Loyalty points"}]} for HKD settlement + LP book.
     */
    @Size(max = 32) List<@Valid AccountOpenSpecDto> accounts
) {
    public CreateWalletOnboardRequestDto {
        if (associatedIdentifier != null) {
            associatedIdentifier = associatedIdentifier.trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (associatedFrom != null) {
            associatedFrom = associatedFrom.trim();
            if (associatedFrom.isEmpty()) {
                associatedFrom = null;
            }
        }
    }

    public CreateWalletOnboardRequestDto(String associatedIdentifier, Currency settlementCurrency, String name) {
        this(associatedIdentifier, settlementCurrency, name, null, null);
    }

    public CreateWalletOnboardRequestDto(
        String associatedIdentifier,
        Currency settlementCurrency,
        String name,
        String associatedFrom
    ) {
        this(associatedIdentifier, settlementCurrency, name, associatedFrom, null);
    }
}
