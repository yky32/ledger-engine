package com.altech.ledger.entity.po.ledger;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Port of the-wallet-ledger {@code Wallet} (account association).
 * <p>
 * New-on-top: {@link #ownerId}, {@link #currency} for CRM onboarding uniqueness.
 */
@Entity
@Table(
    name = "wallet",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniqueWalletKey", columnNames = {
            "account_id", "ext_identifier", "type"
        }),
        @UniqueConstraint(name = "uniqueAlias", columnNames = "alias"),
        @UniqueConstraint(name = "uk_wallet_owner_currency", columnNames = {"owner_id", "currency"})
    }
)
public class Wallet extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 100)
    private String alias;

    @Column(length = 200)
    private String nickname;

    @Column(name = "ext_identifier", length = 100)
    private String extIdentifier;

    @Column(name = "ext_type", length = 50)
    private String extType;

    @Column(length = 66)
    private String hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletAssociationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", length = 20)
    private WalletType walletType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

    // --- engine extensions ---
    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(nullable = false, length = 4)
    private String currency;

    protected Wallet() {}

    public Wallet(Long accountId, String alias, String nickname, String extIdentifier, String extType,
                  WalletAssociationType type, WalletType walletType, WalletStatus status,
                  String ownerId, String currency) {
        this.accountId = accountId;
        this.alias = alias;
        this.nickname = nickname;
        this.extIdentifier = extIdentifier;
        this.extType = extType;
        this.type = type;
        this.walletType = walletType;
        this.status = status;
        this.ownerId = ownerId;
        this.currency = currency;
        this.hash = "wx" + UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getAlias() {
        return alias;
    }

    public String getNickname() {
        return nickname;
    }

    public String getExtIdentifier() {
        return extIdentifier;
    }

    public String getExtType() {
        return extType;
    }

    public String getHash() {
        return hash;
    }

    public WalletAssociationType getType() {
        return type;
    }

    public WalletType getWalletType() {
        return walletType;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setExtIdentifier(String extIdentifier) {
        this.extIdentifier = extIdentifier;
    }

    public void setExtType(String extType) {
        this.extType = extType;
    }

    public void setType(WalletAssociationType type) {
        this.type = type;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getCurrency() {
        return currency;
    }
}
