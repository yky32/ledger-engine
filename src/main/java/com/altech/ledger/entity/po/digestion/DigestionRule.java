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
 * <b>Digestion</b> rule — the webhook <em>brain</em> (filter + scoring).
 * <p>
 * Answers: which {@code eventType} qualifies, min amount / currency / age gates,
 * and which formula turns spend into points (EARN/BURN/PROCESS).
 * Many rows, ordered by {@link #priority}. Editable via {@code /digestion-rules}
 * (no restart). <b>No YAML seed</b> — create via API.
 * <p>
 * <b>Not ingest policy:</b> {@link com.altech.ledger.entity.po.ingest.IngestPolicy}
 * is the door (global on/off + auto-wallet). Digestion assumes the event was allowed in.
 * <p>
 * Storage: intentional flat denormalized row (small hot-path catalog). Business key
 * {@link #code}; PK snowflake {@link #id}.
 *
 * @see com.altech.ledger.entity.po.ingest.IngestPolicy
 * @see com.altech.ledger.entity.po.digestion.package-info
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
