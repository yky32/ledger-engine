package com.altech.ledger.entity.po;

import com.altech.ledger.entity.base.TenancyAware;
import com.altech.ledger.entity.enu.RecipientStatus;
import com.altech.ledger.entity.enu.RecipientTransferChannel;
import jakarta.persistence.*;

/**
 * Port of the-wallet-ledger {@code Recipient}.
 */
@Entity
@Table(name = "recipient")
public class Recipient extends TenancyAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_channel", length = 20)
    private RecipientTransferChannel transferChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipientStatus status = RecipientStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    protected Recipient() {}

    public Recipient(RecipientTransferChannel transferChannel, RecipientStatus status, String metadata) {
        this.transferChannel = transferChannel;
        this.status = status == null ? RecipientStatus.ACTIVE : status;
        this.metadata = metadata;
    }

    public Long getId() { return id; }
    public RecipientTransferChannel getTransferChannel() { return transferChannel; }
    public void setTransferChannel(RecipientTransferChannel transferChannel) { this.transferChannel = transferChannel; }
    public RecipientStatus getStatus() { return status; }
    public void setStatus(RecipientStatus status) { this.status = status; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
