package com.altech.ledger.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.io.Serializable;
import java.time.Instant;

/**
 * Port of {@code com.altech.core.entity.AuditEntity} (standalone, no app-core).
 */
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

    public int getVersion() {
        return version;
    }

    public Instant getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Instant createDt) {
        this.createDt = createDt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(Instant updateDt) {
        this.updateDt = updateDt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
