package com.altech.ledger.entity.po.log;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.LedgerMovementType;
import com.altech.ledger.entity.enu.OrderType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;

/** Business operation log (deposit, earn/burn, hold, …). */
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
@NoArgsConstructor
public class LedgerMovement extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "ledger_movement_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "ledger_movement_id_generator")
    private Long id;

    @Column
    private Long txnId;

    @Column
    private String alias;

    @Column(nullable = false)
    private Long walletId;

    @Column
    private String originatorId;

    @Column
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

    @Column
    private String remarks;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object event;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerMovementType type;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object payerContext;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object recipientContext;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object files;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object complianceContext;

    @Column
    private Long associatedLedgerMovementId;

    @Column(nullable = false, unique = true)
    private String movementKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerMovementMode mode;

    @PrePersist
    void applyDefaults() {
        if (mode == null) {
            mode = LedgerMovementMode.AUTO;
        }
        if (type == null) {
            type = LedgerMovementType.TRANSFER;
        }
        if (alias == null) {
            alias = movementKey;
        }
        if (status == null) {
            status = mode == LedgerMovementMode.AUTO
                ? LedgerMovementStatus.PROCESSING
                : LedgerMovementStatus.PENDING;
        }
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
