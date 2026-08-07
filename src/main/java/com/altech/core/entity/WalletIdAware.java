package com.altech.core.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class WalletIdAware extends AuditEntityWithIsActive {

    private Long walletId;
}
