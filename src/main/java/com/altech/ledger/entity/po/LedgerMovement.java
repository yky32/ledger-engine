package com.altech.ledger.entity.po;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_movement")
public class LedgerMovement {
    public enum OrderType {
        DEPOSIT, WITHDRAWAL, IN_WALLET_TRANSFER, SWIFT_TRANSFER, EARN, BURN, PROCESS, CHARGE
    }
    public enum Status { PENDING, PROCESSING, SETTLED, REJECTED, REVERSED }
    public enum Mode { AUTO, MANUAL }

    @Id
    private UUID id;
    @Column(name = "movement_key", nullable = false, unique = true, length = 150)
    private String movementKey;
    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 30)
    private OrderType orderType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Mode mode;
    @Column(name = "originator_id", length = 100)
    private String originatorId;
    @Column(name = "target_id", length = 100)
    private String targetId;
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;
    @Column(nullable = false, length = 4)
    private String currency;
    @Column(name = "journal_transaction_id")
    private UUID journalTransactionId;
    @Column(columnDefinition = "TEXT")
    private String metadata;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LedgerMovement() {}

    public LedgerMovement(String movementKey, UUID walletId, OrderType orderType, Mode mode,
                          String originatorId, String targetId, BigDecimal amount, String currency,
                          String metadata) {
        this.id = UUID.randomUUID();
        this.movementKey = movementKey;
        this.walletId = walletId;
        this.orderType = orderType;
        this.mode = mode;
        this.status = mode == Mode.AUTO ? Status.SETTLED : Status.PENDING;
        this.originatorId = originatorId;
        this.targetId = targetId;
        this.amount = amount;
        this.currency = currency;
        this.metadata = metadata;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public void markSettled(UUID journalTransactionId) {
        this.status = Status.SETTLED;
        this.journalTransactionId = journalTransactionId;
    }

    public void markProcessing() { this.status = Status.PROCESSING; }
    public void markRejected() { this.status = Status.REJECTED; }

    public UUID getId() { return id; }
    public String getMovementKey() { return movementKey; }
    public UUID getWalletId() { return walletId; }
    public OrderType getOrderType() { return orderType; }
    public Status getStatus() { return status; }
    public Mode getMode() { return mode; }
    public String getOriginatorId() { return originatorId; }
    public String getTargetId() { return targetId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public UUID getJournalTransactionId() { return journalTransactionId; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
