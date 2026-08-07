package com.altech.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;


@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(value = AuditingEntityListener.class)
public class AuditEntity implements Serializable {
    @Version
    @Column
    protected int version;

    @Column
    @CreatedDate
    protected Instant createDt;

    @Column
    @CreatedBy
    protected String createdBy;
    // dynamically use string to store the user key, String.valueOf(user long id)
    // example, "6821123112312578", "1000010201", "UUID"

    @Column
    @LastModifiedDate
    protected Instant updateDt;

    @Column
    @LastModifiedBy
    protected String updatedBy;
}
