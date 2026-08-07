package com.altech.ledger.entity.po.accounting;

import lombok.Getter;
import lombok.Setter;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.persistence.*;

/**
 * Binds an {@link com.altech.ledger.entity.enu.OrderType} to a set of rules for execution.
 * <p>
 * Lookup by order type (DEPOSIT, WITHDRAWAL, …) when a movement runs. Metadata
 * typically lists which {@link Rule} ids apply. Soft path if none configured.
 */
@Entity
@Table(name = "rule_execution")
@Getter
@Setter
public class RuleExecution extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OrderType orderType;

    /** JSON blob — legacy RuleExecutionMetadata (list of rule ids). */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    protected RuleExecution() {}

    public RuleExecution(String name, String description, OrderType orderType, String metadata) {
        this.name = name;
        this.description = description;
        this.orderType = orderType;
        this.metadata = metadata;
    }
}
