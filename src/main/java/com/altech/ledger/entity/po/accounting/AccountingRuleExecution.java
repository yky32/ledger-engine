package com.altech.ledger.entity.po.accounting;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.entity.enu.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Ordered posting sequence: {@code metadata.rules[]} of {@link AccountingRule} ids + seq.
 * Look up by {@code eventType} (Brain / use-case), then by {@code orderType}.
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_accounting_rule_execution_event_type", columnNames = "event_type")
})
@Getter
@Setter
@NoArgsConstructor
public class AccountingRuleExecution extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "accounting_rule_execution_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "accounting_rule_execution_id_generator")
    private Long id;

    @Column(unique = true)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column
    private OrderType orderType;

    /** Brain / SDK eventType this sequence belongs to. Null = default for {@link #orderType}. */
    @Column
    private String eventType;

    /**
     * JSON {@code {"rules":[{"id":"...","seq":1},...]}} — ordered AccountingRule legs.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (eventType != null) {
            String t = eventType.trim().toUpperCase();
            eventType = t.isEmpty() ? null : t;
        }
    }
}
