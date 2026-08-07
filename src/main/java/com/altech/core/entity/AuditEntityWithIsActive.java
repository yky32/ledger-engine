package com.altech.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit base that also tracks soft-active flag. New rows default {@code isActive = true}
 * on persist; deactivate by setting false instead of hard-delete when product needs it.
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
    private void isActive() {
        this.isActive = true;
    }
}
