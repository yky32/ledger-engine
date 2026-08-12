package com.altech.ledger.entity.po.integration;

import com.altech.core.entity.AuditEntityWithIsActive;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Runtime digestion rule: filter + scoring for inbound transactional webhooks.
 * <p>
 * Editable via {@code /digestion-rules}; takes effect without restart.
 * YAML {@code ledger.integration.rules} seeds this table when empty.
 */
@Entity
@Table(
    name = "digestion_rule",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_digestion_rule_code", columnNames = "code")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class DigestionRule extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable business code, e.g. PURCHASE_DEFAULT. */
    @Column(nullable = false, length = 80)
    private String code;

    @Column(length = 200)
    private String name;

    @Column(nullable = false, length = 80)
    private String eventType;

    /** EARN | BURN | PROCESS */
    @Column(nullable = false, length = 20)
    private String operation = "EARN";

    /** When false, rule is skipped. */
    @Column(nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    /** Lower runs first. */
    @Column(nullable = false)
    private Integer priority = 100;

    @Column(precision = 36, scale = 18)
    private BigDecimal minAmount = BigDecimal.ZERO;

    /** Comma-separated ISO codes, e.g. HKD,USD. Blank = any. */
    @Column(length = 500)
    private String eligibleCurrencies;

    /** Null = no age gate. */
    private Integer maxAgeDays;

    @Column(nullable = false, length = 16)
    private String pointCurrency = "LP";

    /**
     * Scoring expression. Supported:
     * <ul>
     *   <li>{@code AMOUNT}</li>
     *   <li>{@code RATE:0.01} → amount * 0.01</li>
     *   <li>{@code FIXED:100}</li>
     *   <li>{@code MUL_ADD:0.01:5} → amount * 0.01 + 5</li>
     *   <li>JSON {@code {"rate":0.01,"fixed":0}} → amount * rate + fixed</li>
     * </ul>
     */
    @Column(nullable = false, length = 500)
    private String formula = "AMOUNT";

    /** PROCESS subtype when operation=PROCESS. */
    @Column(length = 40)
    private String processType;
}
