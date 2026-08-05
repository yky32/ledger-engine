package com.altech.ledger.entity.po;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet")
public class Wallet {
    public enum Status { PENDING, ACTIVE, FROZEN, CLOSED }

    @Id
    private UUID id;
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    @Column(nullable = false, unique = true, length = 100)
    private String alias;
    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;
    @Column(nullable = false, length = 4)
    private String currency;
    @Column(name = "external_id", length = 100)
    private String externalId;
    @Column(name = "external_type", length = 50)
    private String externalType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {}

    public Wallet(UUID accountId, String alias, String ownerId, String currency,
                  String externalId, String externalType, String name, Status status) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.alias = alias;
        this.ownerId = ownerId;
        this.currency = currency;
        this.externalId = externalId;
        this.externalType = externalType;
        this.name = name;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getAlias() { return alias; }
    public String getOwnerId() { return ownerId; }
    public String getCurrency() { return currency; }
    public String getExternalId() { return externalId; }
    public String getExternalType() { return externalType; }
    public Status getStatus() { return status; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void activate() { this.status = Status.ACTIVE; }
    public void freeze() { this.status = Status.FROZEN; }
}
