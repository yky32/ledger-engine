package com.altech.ledger.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Port of payment-gateway {@code TenancyAware}.
 * Tenant is optional here (no platform TenantContextHolder).
 */
@MappedSuperclass
public abstract class TenancyAware extends AuditEntityWithIsActive {

    @Column(name = "tenant_id")
    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
