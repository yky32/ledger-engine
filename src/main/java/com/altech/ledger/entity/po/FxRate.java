package com.altech.ledger.entity.po;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * FX quote from one currency to another (base → target).
 * Supports fiat, LP, and crypto pairs via {@link Currency}.
 */
@Entity
@Table(
    name = "fx_rate",
    uniqueConstraints = @UniqueConstraint(name = "uniqueFxRateKey", columnNames = {"base", "target"})
)
@Getter
@Setter
public class FxRate extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Currency base;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Currency target;

    @Column(nullable = false, precision = 32, scale = 10)
    private BigDecimal rate;

    protected FxRate() {}

    public FxRate(Currency base, Currency target, BigDecimal rate) {
        this.base = base;
        this.target = target;
        this.rate = rate;
    }
}
