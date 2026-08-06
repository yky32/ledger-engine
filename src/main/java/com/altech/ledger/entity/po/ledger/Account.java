package com.altech.ledger.entity.po.ledger;

import lombok.Getter;
import lombok.Setter;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.AccountStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Port of the-wallet-ledger {@code Account} (COA + mutable balances).
 * <p>
 * New-on-top: {@link #allowNegative} for posting policy used by double-entry journal.
 */
@Entity
@Table(
    name = "account",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniqueAccountKey", columnNames = {
            "entity", "type", "sub_type", "main_account", "sub_account", "buffer", "currency"
        }),
        @UniqueConstraint(name = "uniqueMainAccountSubAccount", columnNames = {
            "main_account", "sub_account"
        }),
        @UniqueConstraint(name = "uk_account_full_number", columnNames = "full_number")
    }
)
@Getter
@Setter
public class Account extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String fullNumber; // entity + type + subType + mainAccount + subAccount + buffer

    // === Chart of Account - COA
    @Column(length = 50)
    private String entity;
    @Column(length = 50)
    private String type;
    @Column(length = 50)
    private String subType;

    // === ASSOCIATION
    @Column(length = 100)
    private String mainAccount;
    @Column(length = 200)
    private String subAccount;
    // === ASSOCIATION

    @Column(length = 50)
    private String buffer;

    /** ISO / loyalty unit (USD, LP, …) — string to support non-fiat without platform Currency enum. */
    @Column(nullable = false, length = 4)
    private String currency;
    // === Chart of Account - COA

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal ledgerBalance;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal availableBalance;
    // === BALANCE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    /** Engine extension — posting may refuse negative signed balance when false. */
    @Column(nullable = false)
    private boolean allowNegative;

    protected Account() {}

    public Account(String fullNumber, String entity, String type, String subType,
                   String mainAccount, String subAccount, String buffer, String currency,
                   boolean allowNegative) {
        this.fullNumber = fullNumber;
        this.entity = entity;
        this.type = type;
        this.subType = subType;
        this.mainAccount = mainAccount;
        this.subAccount = subAccount;
        this.buffer = buffer;
        this.currency = currency;
        this.ledgerBalance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
        this.allowNegative = allowNegative;
    }
}
