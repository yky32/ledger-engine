package com.altech.ledger.entity.po.configuration;

import lombok.Getter;
import lombok.Setter;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import jakarta.persistence.*;

/**
 * Port of the-wallet-ledger {@code SystemConfiguration}.
 */
@Entity
@Table(
    name = "system_configuration",
    uniqueConstraints = @UniqueConstraint(name = "uniqueTargetAndScope", columnNames = {"target", "scope"})
)
@Getter
@Setter
public class SystemConfiguration extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String name;

    @Column(length = 100)
    private String target;

    @Column(length = 100)
    private String scope;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    protected SystemConfiguration() {}

    public SystemConfiguration(String name, String target, String scope, String value) {
        this.name = name;
        this.target = target;
        this.scope = scope;
        this.value = value;
    }
}
