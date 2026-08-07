package com.altech.ledger.entity.dto.movement;

import com.altech.core.constant.enu.Currency;

import jakarta.validation.constraints.NotNull;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Owner-centric product movement API ({@code /movements/*}).
 * Nested records only — not a Spring bean.
 */
public final class MovementDto {
    private MovementDto() {}

    /**
     * Owner deposit into their onboarded wallet for a currency.
     */
    public record DepositRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String ownerId,
        @NotNull Currency currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        @Size(max = 500) String description
    ) {
        public DepositRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
        }
    }

    /**
     * Owner withdrawal from their onboarded wallet.
     */
    public record WithdrawalRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String ownerId,
        @NotNull Currency currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        @Size(max = 100) String targetId,
        @Size(max = 500) String description
    ) {
        public WithdrawalRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
        }
    }

    /**
     * Transfer between two owners in the same currency (in-wallet).
     */
    public record InWalletTransferRequest(
        @NotBlank @Size(max = 150) String movementKey,
        @NotBlank @Size(max = 100) String fromOwnerId,
        @NotBlank @Size(max = 100) String toOwnerId,
        @NotNull Currency currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        @Size(max = 500) String description
    ) {
        public InWalletTransferRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
        }
    }

    /**
     * Optional body when settling a MANUAL / pending movement.
     */
    public record SettleMovementRequest(
        @Size(max = 500) String description
    ) {}

    /**
     * Movement summary returned to product clients after create/get/settle.
     */
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
        Currency currency,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
