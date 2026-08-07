package com.altech.ledger.entity.po.log;

import lombok.Getter;
import lombok.Setter;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.LedgerMovementType;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Business operation log for a deposit, withdrawal, transfer, earn/burn, etc.
 * <p>
 * One movement = one intent against a wallet (amount, currency, order type, from/to).
 * {@link #movementKey} is the idempotency key; {@link #mode} is AUTO (settle now) or
 * MANUAL (wait for settle/docs). Status moves PROCESSING → SETTLED / ERROR.
 * Optional TEXT context fields store JSON for payer, files, compliance.
 */
@Entity
@Table(
    name = "ledger_movement",
    uniqueConstraints = {
        // Legacy unique (txn_id, wallet_id) omitted when txn_id is null-heavy; use movement_key instead.
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

    @Column(length = 100)
    private String alias;

    @Column(nullable = false)
    private Long walletId;

    @Column(length = 100)
    private String originatorId;

    @Column(length = 100)
    private String targetId;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Column(nullable = false, length = 4)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LedgerMovementStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(columnDefinition = "TEXT")
    private String event;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
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

    // --- engine extensions ---
    @Column(nullable = false, unique = true, length = 150)
    private String movementKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerMovementMode mode = LedgerMovementMode.AUTO;

    protected LedgerMovement() {}

    public LedgerMovement(String movementKey, Long walletId, OrderType orderType, LedgerMovementMode mode,
                          String originatorId, String targetId, BigDecimal amount, String currency,
                          String metadata) {
        this.movementKey = movementKey;
        this.walletId = walletId;
        this.orderType = orderType;
        this.mode = mode == null ? LedgerMovementMode.AUTO : mode;
        this.status = this.mode == LedgerMovementMode.AUTO
            ? LedgerMovementStatus.SETTLED
            : LedgerMovementStatus.PENDING;
        this.originatorId = originatorId;
        this.targetId = targetId;
        this.amount = amount;
        this.currency = currency;
        this.metadata = metadata;
        this.type = LedgerMovementType.TRANSFER;
        this.alias = movementKey;
    }

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
