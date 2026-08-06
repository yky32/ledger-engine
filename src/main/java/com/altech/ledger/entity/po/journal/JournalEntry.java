package com.altech.ledger.entity.po.journal;

import lombok.Getter;
import lombok.Setter;

import com.altech.ledger.entity.po.ledger.Account;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * New double-entry journal leg (engine layer on top of legacy product POs).
 * Links to ported {@link Account}.
 */
@Entity
@Table(
    name = "journal_entry",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_entry_transaction_sequence",
        columnNames = {"transaction_id", "sequence_number"}
    )
)
@Getter
@Setter
public class JournalEntry {
    public enum Side { DEBIT, CREDIT }

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private JournalTransaction transaction;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side;
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;
    @Column(nullable = false, length = 4)
    private String currency;
    @Column(name = "sequence_number", nullable = false)
    private int sequence;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected JournalEntry() {}

    public JournalEntry(JournalTransaction transaction, Account account, Side side,
                        BigDecimal amount, String currency, int sequence) {
        this.id = UUID.randomUUID();
        this.transaction = transaction;
        this.account = account;
        this.side = side;
        this.amount = amount;
        this.currency = currency;
        this.sequence = sequence;
        this.createdAt = Instant.now();
    }
}
