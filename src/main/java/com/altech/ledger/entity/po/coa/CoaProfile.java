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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * One row = one client COA profile. Flat columns only (no JSONB).
 * All member books share the same segment codes; currency comes from settlement / LP.
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_coa_profile_code", columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
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

    @Column(nullable = false)
    private Boolean isDefault;

    @Column(nullable = false)
    private Boolean isEnabled;

    /** COA entity segment, e.g. 10 or 01 (Bank A). */
    @Column(nullable = false)
    private String entity;

    /** Account type segment, e.g. 20 LIABILITY / 99 Custodian. */
    @Column(nullable = false)
    private String type;

    /** Sub-type segment, e.g. 00 or 21 Fees. */
    @Column(nullable = false)
    private String subType;

    /** Buffer segment, e.g. 00. */
    @Column(nullable = false)
    private String buffer;

    /** Points currency code for LP books (default LP). */
    @Column(nullable = false)
    private String lpCurrency;

    /** PROGRAM pool accounts may go negative. */
    @Column(nullable = false)
    private Boolean poolAllowNegative;

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (code != null) {
            code = code.trim().toUpperCase();
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
        if (lpCurrency == null || lpCurrency.isBlank()) {
            lpCurrency = "LP";
        } else {
            lpCurrency = lpCurrency.trim().toUpperCase();
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
