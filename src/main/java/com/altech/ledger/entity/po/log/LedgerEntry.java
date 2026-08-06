package com.altech.ledger.entity.po.log;

import lombok.Getter;
import lombok.Setter;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.MovementDirection;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Port of the-wallet-ledger {@code LedgerEntry} (movement-side log leg).
 * <p>
 * Distinct from double-entry {@code JournalEntry} (new layer on top).
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

    @Column(nullable = false, length = 4)
    private String currency;

    protected LedgerEntry() {}

    public LedgerEntry(Long txnId, String targetId, BigDecimal amount,
                       MovementDirection direction, String currency) {
        this.txnId = txnId;
        this.targetId = targetId;
        this.amount = amount;
        this.direction = direction;
        this.currency = currency;
    }
}
