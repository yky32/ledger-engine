package com.altech.ledger.entity.dto.event;

import com.altech.core.kafka.BaseEvent;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.enu.SubOrderType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Kafka / internal pipeline payload for a ledger movement lifecycle message.
 * <p>
 * Extends {@link BaseEvent} ({@code requestId}, {@code eventName}). Carries the
 * movement snapshot (ids, amount, order type, status) so consumers can execute
 * balances or notify without reloading all fields from the DB.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LedgerMovementEvent extends BaseEvent {
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
