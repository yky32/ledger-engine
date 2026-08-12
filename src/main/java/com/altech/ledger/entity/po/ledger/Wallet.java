package com.altech.ledger.entity.po.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.WalletStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer wallet — lean model.
 * <p>
 * <b>1 {@code ownerId} (CRM cust) → 1 Wallet</b>. Money lives on linked accounts;
 * this row is identity + lifecycle + default settlement currency.
 * <p>
 * Removed vs legacy: alias, hash, nickname, associatedIdentifier, associatedFrom,
 * association type, walletType. Public API still exposes {@code associatedIdentifier}
 * as an alias of {@code ownerId}.
 */
@Entity
@Table(
    name = "wallet",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_owner", columnNames = "owner_id")
    }
)
@Getter
@Setter
public class Wallet extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Primary account id under this wallet. */
    @Column(nullable = false)
    private Long accountId;

    /**
     * Customer / CRM id — unique. Same value as API {@code associatedIdentifier}.
     */
    @Column(nullable = false, length = 100)
    private String ownerId;

    /** Optional display name. */
    @Column(length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

    /**
     * Default settlement currency. Primary account is opened in this currency at onboard.
     * Not a uniqueness key.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Currency settlementCurrency;
}
