package com.altech.ledger.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class TenancyAware extends AuditEntityWithIsActive {

    @Column(name = "tenant_id")
    private Long tenantId;
}
