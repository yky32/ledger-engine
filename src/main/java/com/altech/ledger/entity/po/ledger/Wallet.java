package com.altech.ledger.entity.po.ledger;

import lombok.Getter;
import lombok.Setter;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Wallet — (account association).
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
@Getter
@Setter
public class Wallet extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 100)
    private String alias;

    @Column(length = 200)
    private String nickname;

    @Column(length = 100)
    private String extIdentifier;

    @Column(length = 50)
    private String extType;

    @Column(length = 66)
    private String hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletAssociationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WalletType walletType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

    // --- engine extensions ---
    @Column(nullable = false, length = 100)
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
}
