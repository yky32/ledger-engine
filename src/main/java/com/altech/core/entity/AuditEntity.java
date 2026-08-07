package com.altech.core.entity;

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
    protected int version;

    protected Instant createDt;

    protected String createdBy;

    protected Instant updateDt;

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
