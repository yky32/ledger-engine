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
 * {@link #extIdentifier} is the sole customer unique key (CRM).
 * Stored as wallet {@code ownerId} and {@code extIdentifier}.
 * <p>
 * Optional {@link #accounts} is free-form (caller / SDK product codes).
 * If omitted or empty, only the primary account is opened.
 */
public record CreateWalletOnboardRequestDto(
    /** Customer unique id (CRM). Sole identity field for create. */
    @NotBlank @Size(max = 100) String extIdentifier,

    @NotNull Currency currency,

    @Size(max = 200) String name,

    /** Source system of extIdentifier; default CRM when blank. */
    @Size(max = 50) String extType,

    /**
     * Accounts to open under this wallet. Free-form ref codes — product catalog
     * is client/SDK concern. Primary always ensured; omit for primary only.
     */
    @Size(max = 32) List<@Valid AccountOpenSpecDto> accounts
) {
    public CreateWalletOnboardRequestDto {
        if (extIdentifier != null) {
            extIdentifier = extIdentifier.trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (extType != null) {
            extType = extType.trim();
            if (extType.isEmpty()) {
                extType = null;
            }
        }
    }

    /** Convenience: primary account only, default CRM type. */
    public CreateWalletOnboardRequestDto(String extIdentifier, Currency currency, String name) {
        this(extIdentifier, currency, name, null, null);
    }

    /** Convenience: no accounts. */
    public CreateWalletOnboardRequestDto(
        String extIdentifier,
        Currency currency,
        String name,
        String extType
    ) {
        this(extIdentifier, currency, name, extType, null);
    }
}
