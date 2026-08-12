package com.altech.ledger.entity.po.configuration;

import com.altech.core.entity.AuditEntityWithIsActive;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Key/value config scoped by target + scope.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uniqueTargetAndScope", columnNames = {"target", "scope"}))
@Getter
@Setter
public class SystemConfiguration extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String target;

    private String scope;

    @Column(columnDefinition = "TEXT")
    private String value;
}
