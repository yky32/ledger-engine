package com.altech.ledger.entity.dto.ledger;

import com.altech.ledger.entity.enu.AccountStatus;
import com.altech.ledger.entity.po.journal.JournalEntry;
import com.altech.ledger.entity.po.journal.JournalTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public record EntryRequest(
        @NotNull Long accountId,
        @NotNull JournalEntry.Side side,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @Positive Integer sequence
    ) {}

    public record PostTransactionRequest(
        @NotBlank @Size(max = 150) String idempotencyKey,
        @Size(max = 150) String reference,
        @Size(max = 500) String description,
        Instant effectiveAt,
        @NotNull @Size(min = 2) List<@Valid EntryRequest> entries
    ) {}

    public record ReversalRequest(
        @NotBlank @Size(max = 150) String idempotencyKey,
        @Size(max = 500) String description
    ) {}

    public record EntryResponse(
        UUID id, UUID transactionId, Long accountId, JournalEntry.Side side, BigDecimal amount,
        String currency, int sequence, Instant createdAt
    ) {}

    public record TransactionResponse(
        UUID id, String idempotencyKey, String reference, String description,
        JournalTransaction.Status status, Instant effectiveAt, Instant createdAt,
        UUID reversalOf, List<EntryResponse> entries
    ) {}

    public record BalanceResponse(
        Long accountId, String currency,
        BigDecimal debitTotal, BigDecimal creditTotal, BigDecimal balance,
        BigDecimal ledgerBalance, BigDecimal availableBalance
    ) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
