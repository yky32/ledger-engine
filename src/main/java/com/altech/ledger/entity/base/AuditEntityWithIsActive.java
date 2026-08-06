package com.altech.ledger.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/**
 * Port of {@code com.altech.core.entity.AuditEntityWithIsActive} (standalone, no app-core).
 */
@MappedSuperclass
public abstract class AuditEntityWithIsActive extends AuditEntity {

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    private void defaultActive() {
        if (isActive == null) {
            isActive = true;
        }
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
