package com.altech.core.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Optional wallet-scoped marker: rows that hang off a single {@code walletId}.
 * Use for child tables that always belong to one wallet.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class WalletIdAware extends AuditEntityWithIsActive {

    private Long walletId;
}
