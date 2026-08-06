package com.altech.ledger.entity.po.log;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.LedgerMovementType;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port of the-wallet-ledger {@code LedgerMovement} (business operation log).
 * <p>
 * New-on-top: {@link #movementKey}, {@link #mode}, {@link #journalTransactionId}, simplified metadata.
 * JSONB context fields from legacy are kept as TEXT for later product parity.
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
public class LedgerMovement extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_id")
    private Long txnId;

    @Column(length = 100)
    private String alias;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "originator_id", length = 100)
    private String originatorId;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Column(nullable = false, length = 4)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 30)
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

    @Column(name = "payer_context", columnDefinition = "TEXT")
    private String payerContext;

    @Column(name = "recipient_context", columnDefinition = "TEXT")
    private String recipientContext;

    @Column(columnDefinition = "TEXT")
    private String files;

    @Column(name = "compliance_context", columnDefinition = "TEXT")
    private String complianceContext;

    @Column(name = "associated_ledger_movement_id")
    private Long associatedLedgerMovementId;

    // --- engine extensions ---
    @Column(name = "movement_key", nullable = false, unique = true, length = 150)
    private String movementKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerMovementMode mode = LedgerMovementMode.AUTO;

    @Column(name = "journal_transaction_id")
    private UUID journalTransactionId;

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

    public void markSettled(UUID journalTransactionId) {
        this.status = LedgerMovementStatus.SETTLED;
        this.journalTransactionId = journalTransactionId;
    }

    public void markProcessing() {
        this.status = LedgerMovementStatus.PROCESSING;
    }

    public void markRejected() {
        this.status = LedgerMovementStatus.REJECTED;
    }

    public void setStatus(LedgerMovementStatus status) {
        this.status = status;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setComplianceContext(String complianceContext) {
        this.complianceContext = complianceContext;
    }

    public void setFiles(String files) {
        this.files = files;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public Long getTxnId() {
        return txnId;
    }

    public void setTxnId(Long txnId) {
        this.txnId = txnId;
    }

    public String getAlias() {
        return alias;
    }

    public Long getWalletId() {
        return walletId;
    }

    public String getOriginatorId() {
        return originatorId;
    }

    public String getTargetId() {
        return targetId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public LedgerMovementStatus getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getEvent() {
        return event;
    }

    public String getMetadata() {
        return metadata;
    }

    public LedgerMovementType getType() {
        return type;
    }

    public String getPayerContext() {
        return payerContext;
    }

    public String getRecipientContext() {
        return recipientContext;
    }

    public String getFiles() {
        return files;
    }

    public String getComplianceContext() {
        return complianceContext;
    }

    public Long getAssociatedLedgerMovementId() {
        return associatedLedgerMovementId;
    }

    public String getMovementKey() {
        return movementKey;
    }

    public LedgerMovementMode getMode() {
        return mode;
    }

    public UUID getJournalTransactionId() {
        return journalTransactionId;
    }
}
