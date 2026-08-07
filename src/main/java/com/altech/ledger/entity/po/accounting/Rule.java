package com.altech.ledger.entity.po.accounting;

import lombok.Getter;
import lombok.Setter;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.MovementDirection;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Reusable accounting rule definition (name, direction, multiplier, target account).
 * <p>
 * Catalog entry that can be referenced by rule-execution configs. Content may hold
 * free-form or JSON payload for product-specific rule logic.
 */
@Entity
@Table(name = "rule")
@Getter
@Setter
public class Rule extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MovementDirection direction;

    @Column(precision = 38, scale = 18)
    private BigDecimal multiplier;

    @Column(length = 200)
    private String targetAccount;

    /** JSON blob — legacy RuleMetadata. */
    @Column(columnDefinition = "TEXT")
    private String content;

    protected Rule() {}

    public Rule(String name, String description, MovementDirection direction,
                BigDecimal multiplier, String targetAccount, String content) {
        this.name = name;
        this.description = description;
        this.direction = direction;
        this.multiplier = multiplier;
        this.targetAccount = targetAccount;
        this.content = content;
    }
}
