package com.altech.ledger.entity.po.accounting;

import com.altech.ledger.entity.base.AuditEntityWithIsActive;
import com.altech.ledger.entity.enu.MovementDirection;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Port of the-wallet-ledger {@code Rule}.
 */
@Entity
@Table(name = "rule")
public class Rule extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MovementDirection direction;

    @Column(precision = 38, scale = 18)
    private BigDecimal multiplier;

    @Column(name = "target_account", length = 200)
    private String targetAccount;

    /** JSON blob — legacy RuleMetadata. */
    @Column(columnDefinition = "TEXT")
    private String content;

    protected Rule() {}

    public Rule(String name, String description, MovementDirection direction,
                BigDecimal multiplier, String targetAccount, String content) {
        this.name = name;
        this.description = description;
        this.direction = direction;
        this.multiplier = multiplier;
        this.targetAccount = targetAccount;
        this.content = content;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MovementDirection getDirection() { return direction; }
    public void setDirection(MovementDirection direction) { this.direction = direction; }
    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
    public String getTargetAccount() { return targetAccount; }
    public void setTargetAccount(String targetAccount) { this.targetAccount = targetAccount; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
