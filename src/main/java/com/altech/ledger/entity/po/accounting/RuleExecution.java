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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class RuleExecution extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "rule_execution_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "rule_execution_id_generator")
    private Long id;

    @Column(unique = true)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column
    private OrderType orderType;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}
