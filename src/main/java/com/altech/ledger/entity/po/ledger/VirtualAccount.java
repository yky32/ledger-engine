package com.altech.ledger.entity.po.ledger;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.VirtualAccountStatus;
import com.altech.ledger.entity.enu.VirtualAccountType;
import jakarta.persistence.*;

/**
 * Port of the-wallet-ledger {@code VirtualAccount}.
 */
@Entity
@Table(
    name = "virtual_account",
    indexes = @Index(name = "idx_virtual_account_ext_identifier", columnList = "ext_identifier")
)
public class VirtualAccount extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ext_identifier", length = 100)
    private String extIdentifier;

    @Column(name = "ext_type", length = 50)
    private String extType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VirtualAccountStatus status = VirtualAccountStatus.PENDING;

    @Column(name = "nick_name", length = 200)
    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VirtualAccountType type;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    protected VirtualAccount() {}

    public VirtualAccount(String extIdentifier, String extType, VirtualAccountType type, String nickName) {
        this.extIdentifier = extIdentifier;
        this.extType = extType;
        this.type = type;
        this.nickName = nickName;
        this.status = VirtualAccountStatus.PENDING;
    }

    public Long getId() { return id; }
    public String getExtIdentifier() { return extIdentifier; }
    public void setExtIdentifier(String extIdentifier) { this.extIdentifier = extIdentifier; }
    public String getExtType() { return extType; }
    public void setExtType(String extType) { this.extType = extType; }
    public VirtualAccountStatus getStatus() { return status; }
    public void setStatus(VirtualAccountStatus status) { this.status = status; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public VirtualAccountType getType() { return type; }
    public void setType(VirtualAccountType type) { this.type = type; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
