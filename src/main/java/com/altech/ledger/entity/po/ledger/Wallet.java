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
 * Customer-facing wallet: links an owner to a primary {@link Account} for one currency.
 * <p>
 * Holds product identity (ownerId, alias, external CRM ids, status). Money lives on the
 * linked account(s); this row is the association + lifecycle (PENDING → ACTIVE, etc.).
 * Unique on owner + currency for onboarding idempotency.
 */
@Entity
@Table(
    name = "wallet",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniqueWalletKey", columnNames = {
            "account_id", "associated_identifier", "type"
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
    @Column(nullable = false, length = 100)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Currency currency;

    /** PG-style public hash: random SHA-256 hex (32 chars) if not set. */
    @PrePersist
    private void generateHash() {
        if (hash == null || hash.isBlank()) {
            hash = RandomHashGenerator.generateRandomHash(32);
        }
    }
}
