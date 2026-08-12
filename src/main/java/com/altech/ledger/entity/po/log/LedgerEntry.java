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
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One balance leg when a {@link LedgerMovement} settles.
 */
@Entity
@Getter
@Setter
public class LedgerEntry extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long txnId;

    private String targetId;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    /** When false, as-of ledger ignores this leg (HOLD/RELEASE). */
    private Boolean affectsLedger;

    /** When false, as-of available ignores this leg. */
    private Boolean affectsAvailable;

    @PrePersist
    void applyDefaults() {
        if (affectsLedger == null) {
            affectsLedger = Boolean.TRUE;
        }
        if (affectsAvailable == null) {
            affectsAvailable = Boolean.TRUE;
        }
    }

    public boolean isAffectsLedger() {
        return affectsLedger == null || affectsLedger;
    }

    public boolean isAffectsAvailable() {
        return affectsAvailable == null || affectsAvailable;
    }
}
