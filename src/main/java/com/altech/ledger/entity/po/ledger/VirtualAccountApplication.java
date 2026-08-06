package com.altech.ledger.entity.po.ledger;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.VirtualAccountApplicationStatus;
import com.altech.ledger.entity.enu.VirtualAccountType;
import jakarta.persistence.*;

/**
 * Port of the-wallet-ledger {@code VirtualAccountApplication}.
 */
@Entity
@Table(name = "virtual_account_application")
public class VirtualAccountApplication extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VirtualAccountApplicationStatus status = VirtualAccountApplicationStatus.PENDING_APPROVAL;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VirtualAccountType type;

    @Column(name = "ext_identifier", length = 100)
    private String extIdentifier;

    @Column(name = "ext_type", length = 50)
    private String extType;

    @Column(length = 500)
    private String remark;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "virtual_account_id", referencedColumnName = "id")
    private VirtualAccount virtualAccount;

    protected VirtualAccountApplication() {}

    public VirtualAccountApplication(VirtualAccountType type, String extIdentifier, String extType) {
        this.type = type;
        this.extIdentifier = extIdentifier;
        this.extType = extType;
        this.status = VirtualAccountApplicationStatus.PENDING_APPROVAL;
    }

    public Long getId() { return id; }
    public VirtualAccountApplicationStatus getStatus() { return status; }
    public void setStatus(VirtualAccountApplicationStatus status) { this.status = status; }
    public VirtualAccountType getType() { return type; }
    public void setType(VirtualAccountType type) { this.type = type; }
    public String getExtIdentifier() { return extIdentifier; }
    public void setExtIdentifier(String extIdentifier) { this.extIdentifier = extIdentifier; }
    public String getExtType() { return extType; }
    public void setExtType(String extType) { this.extType = extType; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public VirtualAccount getVirtualAccount() { return virtualAccount; }
    public void setVirtualAccount(VirtualAccount virtualAccount) { this.virtualAccount = virtualAccount; }
}
