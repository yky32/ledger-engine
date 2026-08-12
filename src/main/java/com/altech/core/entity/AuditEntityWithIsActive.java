package com.altech.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit base + soft-active flag. Defaults {@code isActive = true} only when null.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntityWithIsActive extends AuditEntity {
    @Column(nullable = false)
    private Boolean isActive;

    @PrePersist
    void applyIsActiveDefault() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}
