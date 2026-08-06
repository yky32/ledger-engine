package com.altech.ledger.entity.po.configuration;

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

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
