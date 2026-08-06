package com.altech.ledger.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
