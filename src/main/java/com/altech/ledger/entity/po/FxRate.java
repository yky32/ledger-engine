package com.altech.ledger.entity.po;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

/** FX quote base → target. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uniqueFxRateKey", columnNames = {"base", "target"}))
@Getter
@Setter
@NoArgsConstructor
public class FxRate extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "fx_rate_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "fx_rate_id_generator")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency base;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency target;

    @Column(nullable = false, precision = 32, scale = 10)
    private BigDecimal rate;
}
