package com.altech.ledger.entity.po.ledger;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.AccountStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Port of the-wallet-ledger {@code VirtualSubAccount}.
 */
@Entity
@Table(name = "virtual_sub_account")
public class VirtualSubAccount extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 4)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "ledger_balance", nullable = false, precision = 38, scale = 18)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 38, scale = 18)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "virtual_account_id", referencedColumnName = "id")
    private VirtualAccount virtualAccount;

    protected VirtualSubAccount() {}

    public VirtualSubAccount(VirtualAccount virtualAccount, String currency) {
        this.virtualAccount = virtualAccount;
        this.currency = currency;
        this.status = AccountStatus.ACTIVE;
        this.ledgerBalance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public BigDecimal getLedgerBalance() { return ledgerBalance; }
    public void setLedgerBalance(BigDecimal ledgerBalance) { this.ledgerBalance = ledgerBalance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public VirtualAccount getVirtualAccount() { return virtualAccount; }
}
