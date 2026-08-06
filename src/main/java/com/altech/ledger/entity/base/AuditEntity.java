package com.altech.ledger.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
public abstract class AuditEntity implements Serializable {

    @Version
    @Column(name = "version")
    protected int version;

    @Column(name = "create_dt")
    protected Instant createDt;

    @Column(name = "created_by")
    protected String createdBy;

    @Column(name = "update_dt")
    protected Instant updateDt;

    @Column(name = "updated_by")
    protected String updatedBy;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createDt == null) {
            createDt = now;
        }
        updateDt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateDt = Instant.now();
    }
}
