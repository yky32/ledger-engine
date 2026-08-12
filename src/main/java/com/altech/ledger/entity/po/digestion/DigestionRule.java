package com.altech.ledger.entity.po.digestion;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

/**
 * Digestion rule — filter + scoring (brain). No YAML seed.
 */
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_digestion_rule_code", columnNames = "code")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class DigestionRule extends AuditEntityWithIsActive {

    @Id
    @GenericGenerator(name = "digestion_rule_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "digestion_rule_id_generator")
    private Long id;

    @Column(nullable = false)
    private String code;

    private String name;

    @Column(nullable = false)
    private String eventType;

    /** EARN | BURN | PROCESS */
    @Column(nullable = false)
    private String operation = "EARN";

    @Column(nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(precision = 36, scale = 18)
    private BigDecimal minAmount = BigDecimal.ZERO;

    private String eligibleCurrencies;

    private Integer maxAgeDays;

    @Column(nullable = false)
    private String pointCurrency = "LP";

    @Column(nullable = false)
    private String formula = "AMOUNT";

    private String processType;
}
