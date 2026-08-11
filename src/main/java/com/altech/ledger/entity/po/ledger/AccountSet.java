package com.altech.ledger.entity.po.ledger;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.AccountSetStatus;
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
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Group of CoA accounts under a wallet (Phase A: one DEFAULT set per wallet).
 */
@Entity
@Table(
    name = "account_set",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_set_wallet_code", columnNames = {"wallet_id", "code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class AccountSet extends AuditEntityWithIsActive {

    public static final String CODE_DEFAULT = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    /** Product code within wallet — v1 always {@link #CODE_DEFAULT}. */
    @Column(nullable = false, length = 40)
    private String code = CODE_DEFAULT;

    @Column(length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountSetStatus status = AccountSetStatus.ACTIVE;
}
