package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Product onboarding: create one wallet for a customer + currency.
 * <p>
 * {@link #associatedIdentifier} is the sole associated-party key (e.g. CRM cust id).
 * Stored as wallet {@code ownerId} and {@code associatedIdentifier}.
 * {@link #associatedFrom} names where that id comes from (e.g. CRM).
 * <p>
 * Optional {@link #accounts} is free-form (caller / SDK product codes).
 * If omitted or empty, only the primary account is opened.
 */
public record CreateWalletOnboardRequestDto(
    /** Associated party id (CRM cust id, member id, …). Sole identity for create. */
    @NotBlank @Size(max = 100) String associatedIdentifier,

    @NotNull Currency currency,

    @Size(max = 200) String name,

    /** System that owns associatedIdentifier (e.g. CRM, CORE_BANKING). Default CRM. */
    @Size(max = 50) String associatedFrom,

    /**
     * Accounts to open under this wallet. Free-form ref codes — product catalog
     * is client/SDK concern. Primary always ensured; omit for primary only.
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

    /** Convenience: primary account only, default CRM type. */
    public CreateWalletOnboardRequestDto(String associatedIdentifier, Currency currency, String name) {
        this(associatedIdentifier, currency, name, null, null);
    }

    /** Convenience: no accounts. */
    public CreateWalletOnboardRequestDto(
        String associatedIdentifier,
        Currency currency,
        String name,
        String associatedFrom
    ) {
        this(associatedIdentifier, currency, name, associatedFrom, null);
    }
}
