package com.altech.ledger.entity.po.integration;

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
 * Runtime digestion rule: filter + scoring for inbound transactional webhooks.
 * <p>
 * Editable via {@code /digestion-rules}; takes effect without restart.
 * <b>No YAML / startup seed</b> — rules are created only via API (or explicit ops).
 * <p>
 * <b>Storage shape (intentional denormalization):</b> one flat row per rule.
 * Digestion is a small, hot-path catalog (filters + formula), not a general-purpose
 * multi-table rules engine. Keeping eventType / currencies / formula on the row
 * makes list/evaluate/CRUD simple and fast. Stable business key is {@link #code};
 * PK is snowflake {@link #id}. If factors grow later, prefer a JSON bag column
 * over EAV — not required for current scope.
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
