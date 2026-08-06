package com.altech.ledger.entity.po.ledger;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.WalletApplicationStatus;
import jakarta.persistence.*;

/**
 * Port of the-wallet-ledger {@code WalletApplication}.
 */
@Entity
@Table(
    name = "wallet_application",
    uniqueConstraints = @UniqueConstraint(name = "uniqueWalletApplication", columnNames = "reference_hash")
)
public class WalletApplication extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ext_identifier", length = 100)
    private String extIdentifier;

    @Column(name = "ext_type", length = 50)
    private String extType;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "reference_hash", length = 128)
    private String referenceHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletApplicationStatus status = WalletApplicationStatus.PENDING;

    @Column(name = "fail_counter")
    private Integer failCounter = 0;

    @Column(length = 100)
    private String alias;

    protected WalletApplication() {}

    public WalletApplication(String extIdentifier, String extType, String referenceHash, String alias) {
        this.extIdentifier = extIdentifier;
        this.extType = extType;
        this.referenceHash = referenceHash;
        this.alias = alias;
        this.status = WalletApplicationStatus.PENDING;
        this.failCounter = 0;
    }

    public Long getId() { return id; }
    public String getExtIdentifier() { return extIdentifier; }
    public void setExtIdentifier(String extIdentifier) { this.extIdentifier = extIdentifier; }
    public String getExtType() { return extType; }
    public void setExtType(String extType) { this.extType = extType; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public String getReferenceHash() { return referenceHash; }
    public void setReferenceHash(String referenceHash) { this.referenceHash = referenceHash; }
    public WalletApplicationStatus getStatus() { return status; }
    public void setStatus(WalletApplicationStatus status) { this.status = status; }
    public Integer getFailCounter() { return failCounter; }
    public void setFailCounter(Integer failCounter) { this.failCounter = failCounter; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
}
