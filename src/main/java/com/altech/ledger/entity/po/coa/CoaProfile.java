package com.altech.ledger.entity.po.coa;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * One row = one client COA profile. Flat columns only (no JSONB).
 * Segment / flag defaults via {@code @Builder.Default}.
 * Persist hook only trims/uppercases and fills {@code transactionCode} from {@code code}.
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_coa_profile_code", columnNames = "code"),
    @UniqueConstraint(name = "uk_coa_profile_transaction_code", columnNames = "transaction_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoaProfile extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "coa_profile_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "coa_profile_id_generator")
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column
    private String name;

    /**
     * Business transaction / eventType code bound to this COA.
     * <p><b>Default = same as {@code code}</b> unless operator sets a different value (extension).
     * Null only when explicitly cleared on non-default edge cases.
     */
    @Column
    private String transactionCode;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDefault = Boolean.FALSE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    /** COA entity segment, e.g. 10 or 01. */
    @Builder.Default
    @Column(nullable = false)
    private String entity = "10";

    /** Account type segment, e.g. 20 LIABILITY. */
    @Builder.Default
    @Column(nullable = false)
    private String type = "20";

    /** Sub-type segment. */
    @Builder.Default
    @Column(nullable = false)
    private String subType = "00";

    /** Buffer segment. */
    @Builder.Default
    @Column(nullable = false)
    private String buffer = "00";

    /** Points / loyalty currency for LP books under this profile. */
    @Builder.Default
    @Column(nullable = false)
    private String currency = "LP";

    /** PROGRAM pool accounts may go negative. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean poolAllowNegative = Boolean.TRUE;

    /**
     * House / corporate COA: the one company wallet these books belong to.
     * Member event-type profiles leave this null.
     */
    @Column
    private Long walletId;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (transactionCode != null) {
            String t = transactionCode.trim().toUpperCase();
            transactionCode = t.isEmpty() ? null : t;
        }
        // Default assumption: code == transactionCode (eventType) unless extended
        if (transactionCode == null && code != null && !code.isBlank()) {
            transactionCode = code;
        }
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
    }
}
