package com.altech.ledger.entity.po.accounting;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.MovementDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Reusable accounting rule definition.
 */
@Entity
@Getter
@Setter
public class Rule extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private MovementDirection direction;

    @Column(precision = 38, scale = 18)
    private BigDecimal multiplier;

    private String targetAccount;

    /** JSON blob — legacy RuleMetadata. */
    @Column(columnDefinition = "TEXT")
    private String content;
}
