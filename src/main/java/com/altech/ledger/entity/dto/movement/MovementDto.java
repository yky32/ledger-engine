package com.altech.ledger.entity.dto.movement;

import com.altech.ledger.entity.po.LedgerMovement;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class MovementDto {
    private MovementDto() {}

    public record DepositRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String ownerId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovement.Mode mode,
        @Size(max = 500) String description
    ) {
        public DepositRequest {
            if (mode == null) mode = LedgerMovement.Mode.AUTO;
        }
    }

    public record WithdrawalRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String ownerId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovement.Mode mode,
        @Size(max = 100) String targetId,
        @Size(max = 500) String description
    ) {
        public WithdrawalRequest {
            if (mode == null) mode = LedgerMovement.Mode.AUTO;
        }
    }

    public record InWalletTransferRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String fromOwnerId,
        @NotBlank @Size(max = 100) String toOwnerId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovement.Mode mode,
        @Size(max = 500) String description
    ) {
        public InWalletTransferRequest {
            if (mode == null) mode = LedgerMovement.Mode.AUTO;
        }
    }

    public record SettleMovementRequest(
        @Size(max = 500) String description
    ) {}

    public record MovementResponse(
        UUID id,
        String movementKey,
        UUID walletId,
        LedgerMovement.OrderType orderType,
        LedgerMovement.Status status,
        LedgerMovement.Mode mode,
        String originatorId,
        String targetId,
        BigDecimal amount,
        String currency,
        UUID journalTransactionId,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
