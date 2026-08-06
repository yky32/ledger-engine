package com.altech.ledger.entity.dto.movement;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public final class MovementDto {
    private MovementDto() {}

    public record DepositRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String ownerId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        @Size(max = 500) String description
    ) {
        public DepositRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
        }
    }

    public record WithdrawalRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String ownerId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        @Size(max = 100) String targetId,
        @Size(max = 500) String description
    ) {
        public WithdrawalRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
        }
    }

    public record InWalletTransferRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String fromOwnerId,
        @NotBlank @Size(max = 100) String toOwnerId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        @Size(max = 500) String description
    ) {
        public InWalletTransferRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
        }
    }

    public record SettleMovementRequest(
        @Size(max = 500) String description
    ) {}

    public record MovementResponse(
        Long id,
        String movementKey,
        Long walletId,
        OrderType orderType,
        LedgerMovementStatus status,
        LedgerMovementMode mode,
        String originatorId,
        String targetId,
        BigDecimal amount,
        String currency,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
