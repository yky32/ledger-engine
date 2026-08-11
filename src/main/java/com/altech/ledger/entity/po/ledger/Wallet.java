package com.altech.ledger.entity.po.ledger;

import com.altech.core.constant.enu.Currency;
import lombok.Getter;
import lombok.Setter;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.RandomHashGenerator;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import jakarta.persistence.*;

/**
 * Customer-facing wallet: <b>1 customer ({@code ownerId} / associated id) → 1 wallet</b>.
 * <p>
 * Holds product identity (ownerId, alias, external CRM ids, status). Money lives on the
 * linked account(s); this row is the association + lifecycle (PENDING → ACTIVE, etc.).
 * <p>
 * {@link #settlementCurrency} is the wallet default settlement currency only — not part of
 * wallet identity. Uniqueness is on {@code owner_id} (1 CUST : 1 Wallet).
 */
@Entity
@Table(
    name = "wallet",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniqueWalletKey", columnNames = {
            "account_id", "associated_identifier", "type"
        }),
        @UniqueConstraint(name = "uniqueAlias", columnNames = "alias"),
        @UniqueConstraint(name = "uk_wallet_owner", columnNames = "owner_id")
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
    private String associatedIdentifier;

    @Column(length = 50)
    private String associatedFrom;

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
    /** Customer / CRM id. Unique — one wallet per customer. */
    @Column(nullable = false, length = 100)
    private String ownerId;

    /**
     * Default settlement currency for this wallet.
     * Primary account is opened in this currency at onboard; not a uniqueness key.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_currency", nullable = false, length = 16)
    private Currency settlementCurrency;

    /** PG-style public hash: random SHA-256 hex (32 chars) if not set. */
    @PrePersist
    private void generateHash() {
        if (hash == null || hash.isBlank()) {
            hash = RandomHashGenerator.generateRandomHash(32);
        }
    }
}
