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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * Customer wallet — lean model.
 * <p>
 * <b>1 {@code ownerId} → 1 Wallet</b>. Money on linked accounts.
 * All wallet <b>queries</b> use {@code ownerId} (path / query param).
 * <p>
 * {@link #vanityCode} is optional customer-facing display (lucky / premium number).
 * Never use it as PK, FK, or integration identity — see {@link com.altech.ledger.util.WalletVanityCodes}.
 * <p>
 * Column lengths left to dialect defaults — no hand {@code @Column(length)}.
 */
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_owner", columnNames = "owner_id"),
        @UniqueConstraint(name = "uk_wallet_vanity_code", columnNames = "vanity_code")
    }
)
@Getter
@Setter
public class Wallet extends AuditEntityWithIsActive {

    @Id
    @GenericGenerator(name = "wallet_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "wallet_id_generator")
    private Long id;

    /** Primary account id under this wallet. */
    @Column(nullable = false)
    private Long accountId;

    /** Customer / CRM id — unique. Query key for all wallet GET APIs. */
    @Column(nullable = false)
    private String ownerId;

    /**
     * Optional customer-facing vanity / premium display code (e.g. lucky digits).
     * Mutable over product life; unique when set. Not a system identity.
     */
    private String vanityCode;

    /** Optional display name. */
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

    /**
     * Default settlement currency. Primary account opened in this currency at onboard.
     * Not a uniqueness key.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency settlementCurrency;
}
