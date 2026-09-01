package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.LedgerMovementType;
import com.altech.ledger.entity.enu.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

public record GetLedgerMovementResponseDto(
    Long id,
    String movementKey,
    Long walletId,
    Long txnId,
    String alias,
    String originatorId,
    String targetId,
    BigDecimal amount,
    Currency currency,
    OrderType orderType,
    LedgerMovementStatus status,
    LedgerMovementMode mode,
    LedgerMovementType type,
    String remarks,
    String metadata,
    String complianceContext,
    String files,
    String mainAccount,
    Instant createDt,
    Instant updateDt
) {}
