package com.altech.ledger.entity.po.log;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.MovementDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One balance leg produced when a {@link LedgerMovement} settles.
 * <p>
 * {@link #affectsLedger} / {@link #affectsAvailable} distinguish HOLD (available-only)
 * from true double-entry balance legs — required for as-of rebuild.
 */
@Entity
@Table(name = "ledger_entry")
@Getter
@Setter
public class LedgerEntry extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long txnId;

    @Column(length = 100)
    private String targetId;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Currency currency;

    /** When false, as-of ledger ignores this leg (HOLD/RELEASE). Null = true (legacy). */
    @Column
    private Boolean affectsLedger = Boolean.TRUE;

    /** When false, as-of available ignores this leg. Null = true (legacy). */
    @Column
    private Boolean affectsAvailable = Boolean.TRUE;

    protected LedgerEntry() {}

    public LedgerEntry(Long txnId, String targetId, BigDecimal amount,
                       MovementDirection direction, Currency currency) {
        this(txnId, targetId, amount, direction, currency, true, true);
    }

    public LedgerEntry(Long txnId, String targetId, BigDecimal amount,
                       MovementDirection direction, Currency currency,
                       boolean affectsLedger, boolean affectsAvailable) {
        this.txnId = txnId;
        this.targetId = targetId;
        this.amount = amount;
        this.direction = direction;
        this.currency = currency;
        this.affectsLedger = affectsLedger;
        this.affectsAvailable = affectsAvailable;
    }

    public boolean isAffectsLedger() {
        return affectsLedger == null || affectsLedger;
    }

    public boolean isAffectsAvailable() {
        return affectsAvailable == null || affectsAvailable;
    }
}
