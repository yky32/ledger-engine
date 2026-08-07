package com.altech.ledger.entity.dto.parity;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.LedgerMovementType;
import com.altech.ledger.entity.enu.OrderType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Wallet-id–centric movement API used by {@code /ledger/deposits|withdrawals|transfers}.
 * <p>
 * Distinct from owner-centric {@link com.altech.ledger.entity.dto.movement.MovementDto}.
 * Legacy JSON keys ({@code targetId}, {@code originatorId}) bind via {@link JsonAlias}.
 */
public final class LedgerMovementDtos {
    private LedgerMovementDtos() {}

    /**
     * Deposit into a target wallet (id or alias); optional originator and free-form detail map.
     */
    public record CreateDepositRequest(
        /** Canonical target wallet id (numeric string or alias). */
        @JsonAlias({"targetId"})
        String targetWalletId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        String originatorId,
        @Size(max = 150) String movementKey,
        @Size(max = 500) String description,
        Map<String, Object> detail
    ) {
        public CreateDepositRequest {
            if (mode == null) {
                mode = LedgerMovementMode.AUTO;
            }
            if (currency != null) {
                currency = currency.trim().toUpperCase();
            }
            if (targetWalletId != null) {
                targetWalletId = targetWalletId.trim();
            }
        }

        @JsonIgnore
        public String resolvedTargetWalletId() {
            return blankToNull(targetWalletId);
        }
    }

    /**
     * Withdrawal from an originator wallet; optional external target id.
     */
    public record CreateWithdrawalRequest(
        @JsonAlias({"originatorId"})
        String originatorWalletId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        String targetId,
        @Size(max = 150) String movementKey,
        @Size(max = 500) String description
    ) {
        public CreateWithdrawalRequest {
            if (mode == null) {
                mode = LedgerMovementMode.AUTO;
            }
            if (currency != null) {
                currency = currency.trim().toUpperCase();
            }
            if (originatorWalletId != null) {
                originatorWalletId = originatorWalletId.trim();
            }
        }

        @JsonIgnore
        public String resolvedOriginatorWalletId() {
            return blankToNull(originatorWalletId);
        }
    }

    /**
     * Transfer between two wallet ids (same currency).
     */
    public record CreateInWalletTransferRequest(
        @NotBlank String fromWalletId,
        @NotBlank String toWalletId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        LedgerMovementMode mode,
        @Size(max = 150) String movementKey,
        @Size(max = 500) String description
    ) {
        public CreateInWalletTransferRequest {
            if (mode == null) {
                mode = LedgerMovementMode.AUTO;
            }
            if (currency != null) {
                currency = currency.trim().toUpperCase();
            }
        }
    }

    /**
     * Patch movement status (e.g. force SETTLED / cancel-style transitions).
     */
    public record UpdateStatusRequest(
        @NotNull LedgerMovementStatus status,
        @Size(max = 500) String remarks
    ) {}

    /**
     * Attach document metadata / remarks without changing balances.
     */
    public record UpdateDocumentsRequest(
        String files,
        @Size(max = 500) String remarks
    ) {}

    /**
     * Full movement response for parity clients (status, mode, contexts).
     */
    public record Response(
        Long id,
        String movementKey,
        Long walletId,
        Long txnId,
        String alias,
        String originatorId,
        String targetId,
        BigDecimal amount,
        String currency,
        OrderType orderType,
        LedgerMovementStatus status,
        LedgerMovementMode mode,
        LedgerMovementType type,
        String remarks,
        String metadata,
        String complianceContext,
        String files,
        Instant createDt,
        Instant updateDt
    ) {}

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
