package com.altech.ledger.entity.dto.ledger;

import com.altech.core.constant.enu.Currency;

import com.altech.ledger.entity.enu.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Product ledger-account API shapes (simplified COA by external reference).
 * Nested records only — not a Spring bean.
 */
public final class LedgerDto {
    private LedgerDto() {}

    /** Classic accounting classification stored in Account.type (COA segment). */
    public enum CoaType { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }

    /**
     * Create a product ledger account by external reference + COA type + currency.
     */
    public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String externalReference,
        @NotBlank @Size(max = 200) String name,
        @NotNull CoaType type,
        @NotNull Currency currency,
        boolean allowNegative
    ) {}

    /**
     * Account snapshot returned after create/get (balances + version).
     */
    public record AccountResponse(
        Long id,
        String externalReference,
        String name,
        CoaType type,
        Currency currency,
        AccountStatus status,
        boolean allowNegative,
        BigDecimal ledgerBalance,
        BigDecimal availableBalance,
        int version,
        Instant createdAt,
        Instant updatedAt
    ) {}

    /**
     * Compact balance view for one account id.
     */
    public record BalanceResponse(
        Long accountId,
        Currency currency,
        BigDecimal ledgerBalance,
        BigDecimal availableBalance
    ) {}

    /**
     * Generic page envelope for list APIs (content + pagination numbers).
     */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
