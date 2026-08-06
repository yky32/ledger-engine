package com.altech.ledger.entity.po;

import com.altech.ledger.entity.base.TenancyAware;
import com.altech.ledger.entity.enu.RecipientStatus;
import jakarta.persistence.*;

/**
 * Port of the-wallet-ledger {@code LinkedBankAccount}.
 */
@Entity
@Table(name = "linked_bank_account")
public class LinkedBankAccount extends TenancyAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipientStatus status = RecipientStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    protected LinkedBankAccount() {}

    public LinkedBankAccount(RecipientStatus status, String metadata) {
        this.status = status == null ? RecipientStatus.ACTIVE : status;
        this.metadata = metadata;
    }

    public Long getId() { return id; }
    public RecipientStatus getStatus() { return status; }
    public void setStatus(RecipientStatus status) { this.status = status; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
