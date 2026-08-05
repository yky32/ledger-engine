package com.altech.ledger.entity.po;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_account")
public class LedgerAccount {
    public enum Type { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }
    public enum Status { ACTIVE, FROZEN, CLOSED }

    @Id
    private UUID id;
    @Column(name = "external_reference", nullable = false, unique = true, length = 100)
    private String externalReference;
    @Column(nullable = false, length = 200)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;
    @Column(nullable = false, length = 4)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    @Column(name = "allow_negative", nullable = false)
    private boolean allowNegative;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LedgerAccount() {}

    public LedgerAccount(String externalReference, String name, Type type, String currency, boolean allowNegative) {
        this.id = UUID.randomUUID();
        this.externalReference = externalReference;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.status = Status.ACTIVE;
        this.allowNegative = allowNegative;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getExternalReference() { return externalReference; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public String getCurrency() { return currency; }
    public Status getStatus() { return status; }
    public boolean isAllowNegative() { return allowNegative; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
