package com.altech.ledger.entity.dto.event;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.enu.SubOrderType;

import java.math.BigDecimal;

/**
 * Port of payment-gateway LedgerMovementEvent (standalone POJO).
 */
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

    public Long getMovementId() { return movementId; }
    public void setMovementId(Long movementId) { this.movementId = movementId; }
    public String getMovementKey() { return movementKey; }
    public void setMovementKey(String movementKey) { this.movementKey = movementKey; }
    public Long getBelongToWalletId() { return belongToWalletId; }
    public void setBelongToWalletId(Long belongToWalletId) { this.belongToWalletId = belongToWalletId; }
    public String getOriginatorId() { return originatorId; }
    public void setOriginatorId(String originatorId) { this.originatorId = originatorId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public SubOrderType getSubOrderType() { return subOrderType; }
    public void setSubOrderType(SubOrderType subOrderType) { this.subOrderType = subOrderType; }
    public LedgerMovementMode getMode() { return mode; }
    public void setMode(LedgerMovementMode mode) { this.mode = mode; }
    public LedgerMovementStatus getStatus() { return status; }
    public void setStatus(LedgerMovementStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFiles() { return files; }
    public void setFiles(String files) { this.files = files; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
