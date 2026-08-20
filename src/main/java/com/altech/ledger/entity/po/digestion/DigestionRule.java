package com.altech.ledger.entity.po.digestion;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.usecase.digestion.DigestionFormulaConfig;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Digestion rule — filter + scoring (Brain). No YAML seed.
 * <p>
 * {@link #formula} is JSONB config, e.g. {@code {"type":"RATE","rate":0.01}}.
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_digestion_rule_code", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
public class DigestionRule extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "digestion_rule_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "digestion_rule_id_generator")
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column
    private String name;

    @Column(nullable = false)
    private String eventType;

    /** EARN | BURN | PROCESS */
    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private Boolean isEnabled;

    @Column(nullable = false)
    private Integer priority;

    @Column(precision = 36, scale = 18)
    private BigDecimal minAmount;

    @Column
    private String eligibleCurrencies;

    /**
     * Optional MCC allow-list (CSV), e.g. {@code 5411,5812}.
     * Blank = any MCC. Event MCC read from metadata keys {@code mcc} / {@code mccCode} / {@code merchantCategoryCode}.
     */
    @Column
    private String eligibleMccs;

    @Column
    private Integer maxAgeDays;

    @Column(nullable = false)
    private String pointCurrency;

    /**
     * Scoring config (JSONB). See {@link DigestionFormulaConfig}.
     * Example: {@code {"type":"RATE","rate":0.01,"multiplier":2}}
     */
    @Type(JsonBinaryType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> formula;

    /**
     * Explicit when-factors (JSONB array). ANDed with legacy column filters at evaluate time.
     * See docs/FACTORS.md.
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> whenFactors;

    @Column
    private String processType;

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (operation == null) {
            operation = "EARN";
        }
        if (isEnabled == null) {
            isEnabled = Boolean.TRUE;
        }
        if (priority == null) {
            priority = 100;
        }
        if (minAmount == null) {
            minAmount = BigDecimal.ZERO;
        }
        if (pointCurrency == null) {
            pointCurrency = "LP";
        }
        if (formula == null) {
            formula = DigestionFormulaConfig.ofAmount();
        } else {
            formula = DigestionFormulaConfig.normalize(formula);
        }
    }
}
