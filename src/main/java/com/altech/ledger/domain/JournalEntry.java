package com.altech.ledger.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_entry",
       uniqueConstraints = @UniqueConstraint(name = "uk_entry_transaction_sequence",
                                             columnNames = {"transaction_id", "sequence_number"}))
public class JournalEntry {
    public enum Side { DEBIT, CREDIT }

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private JournalTransaction transaction;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccount account;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side;
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "sequence_number", nullable = false)
    private int sequence;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected JournalEntry() {}

    public JournalEntry(JournalTransaction transaction, LedgerAccount account, Side side,
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

    public UUID getId() { return id; }
    public JournalTransaction getTransaction() { return transaction; }
    public LedgerAccount getAccount() { return account; }
    public Side getSide() { return side; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public int getSequence() { return sequence; }
    public Instant getCreatedAt() { return createdAt; }
}
