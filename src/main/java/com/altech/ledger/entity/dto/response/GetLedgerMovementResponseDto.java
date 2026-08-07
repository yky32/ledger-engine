package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.LedgerMovementType;
import com.altech.ledger.entity.enu.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Full movement response (status, mode, contexts).
 */
public record GetLedgerMovementResponseDto(
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
