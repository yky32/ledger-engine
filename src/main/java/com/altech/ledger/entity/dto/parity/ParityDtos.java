package com.altech.ledger.entity.dto.parity;

import com.altech.ledger.entity.enu.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request/response DTOs mirroring the-wallet-ledger API surface (standalone).
 */
public final class ParityDtos {
    private ParityDtos() {}

    // ---- Account ----
    public record CreateLedgerAccountRequest(
        String entity,
        String type,
        String subType,
        String buffer,
        String mainAccount,
        String subAccount,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        Boolean allowNegative
    ) {}

    public record AccountResponse(
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

    // ---- Wallet ----
    public record CreateLedgerWalletRequest(
        Long accountId,
        String extIdentifier,
        String extType,
        WalletAssociationType type,
        String ownerId,
        String currency,
        String nickname
    ) {}

    public record UpdateLedgerWalletRequest(
        WalletStatus status,
        Long accountId,
        String extIdentifier,
        WalletAssociationType type,
        String nickname
    ) {}

    public record LedgerWalletActivationRequest(
        String accountId,
        String workflowExecutionId
    ) {}

    public record WalletWithBalancesResponse(
        Long id,
        String alias,
        Long accountId,
        String nickname,
        String extIdentifier,
        String extType,
        WalletAssociationType type,
        WalletType walletType,
        WalletStatus status,
        String ownerId,
        String currency,
        List<AccountResponse> accounts,
        Instant createDt,
        Instant updateDt
    ) {}

    // ---- Movements ----
    /**
     * Deposit request. Accepts engine field {@code targetWalletId} or legacy {@code targetId}.
     * Amount may be sent as number (preferred) — JSON binding uses BigDecimal.
     */
    public record CreateDepositRequest(
        String targetWalletId,
        String targetId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull BigDecimal amount,
        LedgerMovementMode mode,
        String originatorId,
        String movementKey,
        String description,
        Map<String, Object> detail
    ) {
        public CreateDepositRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
            if ((targetWalletId == null || targetWalletId.isBlank()) && targetId != null) {
                targetWalletId = targetId;
            }
            if ((targetId == null || targetId.isBlank()) && targetWalletId != null) {
                targetId = targetWalletId;
            }
        }

        /** Canonical wallet id for processing. */
        public String resolvedTargetWalletId() {
            if (targetWalletId != null && !targetWalletId.isBlank()) return targetWalletId;
            return targetId;
        }

        // compact ctor overload used by existing call sites
        public CreateDepositRequest(String targetWalletId, String currency, BigDecimal amount,
                                    LedgerMovementMode mode, String originatorId, String movementKey,
                                    String description, Map<String, Object> detail) {
            this(targetWalletId, targetWalletId, currency, amount, mode, originatorId, movementKey, description, detail);
        }
    }

    public record CreateWithdrawalRequest(
        String originatorWalletId,
        String originatorId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull BigDecimal amount,
        LedgerMovementMode mode,
        String targetId,
        String movementKey,
        String description
    ) {
        public CreateWithdrawalRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
            if ((originatorWalletId == null || originatorWalletId.isBlank()) && originatorId != null) {
                originatorWalletId = originatorId;
            }
            if ((originatorId == null || originatorId.isBlank()) && originatorWalletId != null) {
                originatorId = originatorWalletId;
            }
        }

        public String resolvedOriginatorWalletId() {
            if (originatorWalletId != null && !originatorWalletId.isBlank()) return originatorWalletId;
            return originatorId;
        }

        public CreateWithdrawalRequest(String originatorWalletId, String currency, BigDecimal amount,
                                       LedgerMovementMode mode, String targetId, String movementKey,
                                       String description) {
            this(originatorWalletId, originatorWalletId, currency, amount, mode, targetId, movementKey, description);
        }
    }

    public record CreateInWalletTransferRequest(
        @NotBlank String fromWalletId,
        @NotBlank String toWalletId,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
        @NotNull BigDecimal amount,
        LedgerMovementMode mode,
        String movementKey,
        String description
    ) {
        public CreateInWalletTransferRequest {
            if (mode == null) mode = LedgerMovementMode.AUTO;
        }
    }

    public record UpdateMovementStatusRequest(
        @NotNull LedgerMovementStatus status,
        String remarks
    ) {}

    public record UpdateTransferDocumentsRequest(
        String files,
        String remarks
    ) {}

    public record MovementResponse(
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

    // ---- Rules ----
    public record CreateRuleRequest(
        @NotBlank String name,
        String description,
        MovementDirection direction,
        BigDecimal multiplier,
        String targetAccount,
        String content
    ) {}

    public record RuleResponse(
        Long id, String name, String description, MovementDirection direction,
        BigDecimal multiplier, String targetAccount, String content,
        Instant createDt
    ) {}

    public record CreateRuleExecutionRequest(
        @NotBlank String name,
        String description,
        @NotNull OrderType orderType,
        String metadata
    ) {}

    public record RuleExecutionResponse(
        Long id, String name, String description, OrderType orderType, String metadata, Instant createDt
    ) {}

    // ---- FX / config / dashboard ----
    public record CreateFxRateRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String base,
        @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String target,
        @NotNull BigDecimal rate
    ) {}

    public record FxRateResponse(
        Long id, String base, String target, BigDecimal rate, Instant createDt, Instant updateDt
    ) {}

    public record ConfigurationResponse(
        Long id, String name, String target, String scope, String value
    ) {}

    public record DashboardResponse(
        long walletCount,
        long accountCount,
        long movementCount,
        long openMovementCount
    ) {}

    }
