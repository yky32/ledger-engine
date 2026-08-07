package com.altech.core.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Optional multi-tenant marker: rows that belong to a {@code tenantId}.
 * Extend this when an entity is scoped per tenant rather than global.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenancyAware extends AuditEntityWithIsActive {

    private Long tenantId;
}
