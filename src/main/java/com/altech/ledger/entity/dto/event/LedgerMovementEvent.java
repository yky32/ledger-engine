package com.altech.ledger.entity.dto.event;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.enu.SubOrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Kafka / pipeline movement event payload.
 */
@Getter
@Setter
public class LedgerMovementEvent {
    private Long movementId;
    private String movementKey;
    private Long belongToWalletId;
    private String originatorId;
    private String targetId;
    private BigDecimal amount;
    private String currency;
    private OrderType orderType;
    private SubOrderType subOrderType;
    private LedgerMovementMode mode = LedgerMovementMode.AUTO;
    private LedgerMovementStatus status = LedgerMovementStatus.PROCESSING;
    private String description;
    private String files;
    private String metadata;
}
