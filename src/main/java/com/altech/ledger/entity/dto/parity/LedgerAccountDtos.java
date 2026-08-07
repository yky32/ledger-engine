package com.altech.ledger.entity.dto.parity;

import com.altech.ledger.entity.enu.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Hierarchical chart-of-accounts API (entity / type / subType / buffer / main / sub).
 * Distinct from the simplified {@link com.altech.ledger.entity.dto.ledger.LedgerDto} COA surface.
 */
public final class LedgerAccountDtos {
    private LedgerAccountDtos() {}

    public record CreateRequest(
        String entity,
        String type,
        String subType,
        String buffer,
        String mainAccount,
        String subAccount,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        Boolean allowNegative
    ) {
        public CreateRequest {
            if (currency != null) {
                currency = currency.trim().toUpperCase();
            }
            if (allowNegative == null) {
                allowNegative = Boolean.FALSE;
            }
        }
    }

    public record Response(
        Long id,
        String fullNumber,
        String entity,
        String type,
        String subType,
        String mainAccount,
        String subAccount,
        String buffer,
        String currency,
        BigDecimal ledgerBalance,
        BigDecimal availableBalance,
        AccountStatus status,
        Instant createDt,
        Instant updateDt
    ) {}

    public record BalanceResponse(
        Long accountId,
        String currency,
        BigDecimal ledgerBalance,
        BigDecimal availableBalance,
        BigDecimal fxConvertedBalance
    ) {}
}
