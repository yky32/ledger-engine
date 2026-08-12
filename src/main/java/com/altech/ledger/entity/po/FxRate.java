package com.altech.ledger.entity.po;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * FX quote from one currency to another (base → target).
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uniqueFxRateKey", columnNames = {"base", "target"}))
@Getter
@Setter
public class FxRate extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
