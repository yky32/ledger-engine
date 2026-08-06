package com.altech.ledger.entity.po;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Port of the-wallet-ledger {@code FxRate}.
 * Currency codes are strings (supports non-fiat later).
 */
@Entity
@Table(
    name = "fx_rate",
    uniqueConstraints = @UniqueConstraint(name = "uniqueFxRateKey", columnNames = {"base", "target"})
)
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

    public Long getId() { return id; }
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}
