package com.altech.ledger.entity.dto.ledger;

import com.altech.ledger.entity.enu.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class LedgerDto {
    private LedgerDto() {}

    /** Classic accounting classification stored in Account.type (COA segment). */
    public enum CoaType { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }

    public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String externalReference,
        @NotBlank @Size(max = 200) String name,
        @NotNull CoaType type,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        boolean allowNegative
    ) {}

    public record AccountResponse(
        Long id,
        String externalReference,
        String name,
        CoaType type,
        String currency,
        AccountStatus status,
        boolean allowNegative,
        BigDecimal ledgerBalance,
        BigDecimal availableBalance,
        int version,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record BalanceResponse(
        Long accountId,
        String currency,
        BigDecimal ledgerBalance,
        BigDecimal availableBalance
    ) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
