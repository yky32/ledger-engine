package com.altech.ledger.api;

import com.altech.ledger.domain.JournalEntry;
import com.altech.ledger.domain.JournalTransaction;
import com.altech.ledger.domain.LedgerAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LedgerDtos {
    private LedgerDtos() {}

    public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String externalReference,
        @NotBlank @Size(max = 200) String name,
        @NotNull LedgerAccount.Type type,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        boolean allowNegative
    ) {}

    public record AccountResponse(
        UUID id, String externalReference, String name, LedgerAccount.Type type, String currency,
        LedgerAccount.Status status, boolean allowNegative, long version, Instant createdAt, Instant updatedAt
    ) {}

    public record EntryRequest(
        @NotNull UUID accountId,
        @NotNull JournalEntry.Side side,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
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
        UUID id, UUID transactionId, UUID accountId, JournalEntry.Side side, BigDecimal amount,
        String currency, int sequence, Instant createdAt
    ) {}

    public record TransactionResponse(
        UUID id, String idempotencyKey, String reference, String description,
        JournalTransaction.Status status, Instant effectiveAt, Instant createdAt,
        UUID reversalOf, List<EntryResponse> entries
    ) {}

    public record BalanceResponse(
        UUID accountId, String currency, BigDecimal debitTotal, BigDecimal creditTotal,
        BigDecimal balance
    ) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
