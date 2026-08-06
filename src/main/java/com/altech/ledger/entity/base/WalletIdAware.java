package com.altech.ledger.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Port of payment-gateway {@code WalletIdAware} (without tenancy filters / app-core).
 */
@MappedSuperclass
public abstract class WalletIdAware extends AuditEntityWithIsActive {

    @Column(name = "wallet_id")
    private Long walletId;

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }
}
