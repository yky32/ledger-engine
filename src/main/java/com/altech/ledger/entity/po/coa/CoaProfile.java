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
 * Segment defaults via {@link Builder.Default}; normalize on persist.
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

    /**
     * Points / loyalty currency for LP books under this profile.
     * Column name {@code currency} (was lp_currency).
     */
    @Builder.Default
    @Column(name = "currency", nullable = false)
    private String currency = "LP";

    /** PROGRAM pool accounts may go negative. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean poolAllowNegative = Boolean.TRUE;

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
        if (isDefault == null) {
            isDefault = Boolean.FALSE;
        }
        if (isEnabled == null) {
            isEnabled = Boolean.TRUE;
        }
        if (entity == null || entity.isBlank()) {
            entity = "10";
        }
        if (type == null || type.isBlank()) {
            type = "20";
        }
        if (subType == null || subType.isBlank()) {
            subType = "00";
        }
        if (buffer == null || buffer.isBlank()) {
            buffer = "00";
        }
        if (currency == null || currency.isBlank()) {
            currency = "LP";
        } else {
            currency = currency.trim().toUpperCase();
        }
        if (poolAllowNegative == null) {
            poolAllowNegative = Boolean.TRUE;
        }
        entity = digits(entity, "10");
        type = digits(type, "20");
        subType = digits(subType, "00");
        buffer = digits(buffer, "00");
    }

    private static String digits(String v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = v.trim();
        return s.matches("\\d+") ? s : fallback;
    }
}
