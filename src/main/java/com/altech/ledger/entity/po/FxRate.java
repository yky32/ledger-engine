package com.altech.ledger.entity.po;

import lombok.Getter;
import lombok.Setter;

import com.altech.core.entity.AuditEntityWithIsActive;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * FX quote from one currency code to another (base → target).
 * <p>
 * Used for display conversion on wallet balances and amount convert helpers.
 * Codes are plain strings (USD, LP, …) so non-fiat units work without an enum.
 * Unique pair (base, target).
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

    @Column(nullable = false, length = 4)
    private String base;

    @Column(nullable = false, length = 4)
    private String target;

    @Column(nullable = false, precision = 32, scale = 10)
    private BigDecimal rate;

    protected FxRate() {}

    public FxRate(String base, String target, BigDecimal rate) {
        this.base = base;
        this.target = target;
        this.rate = rate;
    }
}
