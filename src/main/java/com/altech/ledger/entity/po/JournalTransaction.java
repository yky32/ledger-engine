package com.altech.ledger.entity.po;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_transaction")
public class JournalTransaction {
    public enum Status { POSTED, REVERSED }

    @Id
    private UUID id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 150)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(length = 150)
    private String reference;
    @Column(length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private JournalTransaction reversalOf;
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<JournalEntry> entries = new ArrayList<>();

    protected JournalTransaction() {}

    public JournalTransaction(String idempotencyKey, String requestHash, String reference,
                              String description, Instant effectiveAt, JournalTransaction reversalOf) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.reference = reference;
        this.description = description;
        this.status = Status.POSTED;
        this.effectiveAt = effectiveAt;
        this.createdAt = Instant.now();
        this.reversalOf = reversalOf;
    }

    public void addEntry(JournalEntry entry) { entries.add(entry); }
    public void markReversed() { status = Status.REVERSED; }
    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getReference() { return reference; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public Instant getCreatedAt() { return createdAt; }
    public JournalTransaction getReversalOf() { return reversalOf; }
    public List<JournalEntry> getEntries() { return Collections.unmodifiableList(entries); }
}
