package com.altech.ledger.entity.po.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * Customer wallet — lean model. 1 ownerId → 1 Wallet.
 * vanityCode = customer display only (not identity).
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_wallet_owner", columnNames = "owner_id"),
    @UniqueConstraint(name = "uk_wallet_vanity_code", columnNames = "vanity_code")
})
@Getter
@Setter
@NoArgsConstructor
public class Wallet extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "wallet_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "wallet_id_generator")
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private String ownerId;

    @Column
    private String vanityCode;

    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletAssociationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletType walletType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency settlementCurrency;

    /**
     * COA profile code used at onboard (product stream), e.g. DEFAULT / UAF_CC / UAF_LOAN.
     * Null on legacy rows → treat as default when ensuring books.
     */
    @Column
    private String coaProfileCode;

    @PrePersist
    void applyDefaults() {
        if (type == null) {
            type = WalletAssociationType.CUSTODIAN;
        }
        if (walletType == null) {
            walletType = WalletType.INDIVIDUAL;
        }
        if (status == null) {
            status = WalletStatus.ACTIVE;
        }
        if (coaProfileCode != null) {
            coaProfileCode = coaProfileCode.trim().toUpperCase();
            if (coaProfileCode.isEmpty()) {
                coaProfileCode = null;
            }
        }
    }
}
