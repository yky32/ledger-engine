package com.altech.ledger.entity.po.accounting;

import lombok.Getter;
import lombok.Setter;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.persistence.*;

/**
 * Port of the-wallet-ledger {@code RuleExecution}.
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
