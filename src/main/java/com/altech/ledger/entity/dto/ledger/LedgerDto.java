package com.altech.ledger.entity.dto.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;
import com.altech.ledger.entity.enu.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Product ledger-account API shapes (simplified COA by external reference).
 * Nested records only — not a Spring bean.
 */
public final class LedgerDto {
    private LedgerDto() {}

    /** Classic accounting classification stored in Account.type (COA segment). */
    public enum CoaType {
        ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE,
        ;

        public static CoaType get(String input) {
            if (input != null) {
                for (CoaType value : CoaType.values()) {
                    if (input.equalsIgnoreCase(value.name())) {
                        return value;
                    }
                }
            }
            String message = String.format("Wrong [%s] value. [%s] not in -> %s",
                input, input, Arrays.asList(CoaType.values()));
            throw new BizException(SystemResponse.PAM0400, message);
        }
    }

    /**
     * Create a product ledger account (fullNumber allocated as numeric COA).
     */
    public record CreateAccountRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull CoaType type,
        @NotNull Currency currency,
        boolean allowNegative
    ) {}

    /**
     * Account snapshot returned after create/get (balances + version).
     * {@code fullNumber} matches DB {@code account.full_number}.
     */
    public record AccountResponse(
        Long id,
        String fullNumber,
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
