package com.altech.ledger.entity.po.log;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.LedgerMovementType;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Business operation log for a deposit, withdrawal, transfer, earn/burn, etc.
 * <p>
 * {@link #movementKey} is the idempotency key; {@link #mode} is AUTO or MANUAL.
 */
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_movement_key", columnNames = "movement_key")
    },
    indexes = {
        @Index(name = "ledger_movement_idx_walletId", columnList = "wallet_id")
    }
)
@Getter
@Setter
public class LedgerMovement extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long txnId;

    private String alias;

    @Column(nullable = false)
    private Long walletId;

    private String originatorId;

    private String targetId;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerMovementStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(columnDefinition = "TEXT")
    private String event;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerMovementType type = LedgerMovementType.TRANSFER;

    @Column(columnDefinition = "TEXT")
    private String payerContext;

    @Column(columnDefinition = "TEXT")
    private String recipientContext;

    @Column(columnDefinition = "TEXT")
    private String files;

    @Column(columnDefinition = "TEXT")
    private String complianceContext;

    private Long associatedLedgerMovementId;

    @Column(nullable = false, unique = true)
    private String movementKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerMovementMode mode = LedgerMovementMode.AUTO;

    public void markSettled() {
        this.status = LedgerMovementStatus.SETTLED;
    }

    public void markProcessing() {
        this.status = LedgerMovementStatus.PROCESSING;
    }

    public void markRejected() {
        this.status = LedgerMovementStatus.REJECTED;
    }
}
