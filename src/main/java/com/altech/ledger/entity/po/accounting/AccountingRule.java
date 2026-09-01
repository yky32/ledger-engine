package com.altech.ledger.entity.po.accounting;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.entity.enu.MovementDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

/**
 * One posting-sequence leg.
 * {@code targetAccount} is a {@code CoaProfile.code} (chart structure), never a member account id.
 * Member books are resolved at runtime from the movement wallet + that COA.
 * {@code content} kept as TEXT for now (API still string; promote to jsonb when payload is structured).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class AccountingRule extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "accounting_rule_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "accounting_rule_id_generator")
    private Long id;

    @Column
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column
    private MovementDirection direction;

    @Column(precision = 38, scale = 18)
    private BigDecimal multiplier;

    /** CoaProfile.code — chart pointer. Not a snowflake account id. */
    @Column
    private String targetAccount;

    @Column(columnDefinition = "TEXT")
    private String content;
}
