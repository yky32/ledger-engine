package com.altech.ledger.entity.dto.request;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.WalletAccountRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Product onboarding: create one wallet for a user + currency.
 * <p>
 * Optional {@link #accountSet} indicates which account kinds to open under the wallet.
 * If omitted or empty, only {@link WalletAccountRole#MAIN} is created (backward compatible).
 * For prod bulk convert, pass product mix without SQL, e.g.
 * {@code ["MAIN","LOAN","CC_PURPLE"]} for loan + purple card.
 */
public record CreateWalletOnboardRequestDto(
    @NotBlank @Size(max = 100) String userId,
    @NotNull Currency currency,
    @Size(max = 200) String name,
    @Size(max = 100) String externalId,
    @Size(max = 50) String externalType,
    /**
     * Flexible account-set indication. Order is preserved; MAIN is always ensured first.
     * Shorthand: list of roles only — use {@link AccountOpenSpecDto#of(WalletAccountRole)}.
     */
    @Size(max = 32) List<@Valid AccountOpenSpecDto> accountSet
) {
    public CreateWalletOnboardRequestDto {
        if (userId != null) {
            userId = userId.trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (externalId != null) {
            externalId = externalId.trim();
        }
        if (externalType != null) {
            externalType = externalType.trim();
        }
    }

    /** Convenience: no accountSet (MAIN only). */
    public CreateWalletOnboardRequestDto(
        String userId,
        Currency currency,
        String name,
        String externalId,
        String externalType
    ) {
        this(userId, currency, name, externalId, externalType, null);
    }
}
