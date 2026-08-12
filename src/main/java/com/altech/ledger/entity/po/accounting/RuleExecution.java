package com.altech.ledger.entity.po.accounting;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * Binds an {@link OrderType} to a set of rules for execution.
 */
@Entity
@Getter
@Setter
public class RuleExecution extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    /** JSON blob — legacy RuleExecutionMetadata (list of rule ids). */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
