package com.altech.ledger.entity.po.coa;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
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
import org.hibernate.annotations.Type;

import com.altech.ledger.usecase.coa.CoaBindings;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One row = one client COA profile. Role segments live in {@link #bindings} JSONB.
 * Simple Phase-1: no separate entity/type dictionary tables.
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

    /**
     * Keys: MEMBER_SETTLEMENT | MEMBER_LP | PROGRAM_POOL (JSON object).
     * Stored as Object for Hypersistence jsonb (same pattern as SystemConfiguration).
     */
    @Type(JsonBinaryType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Object bindings;

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
        if (bindings == null) {
            bindings = new LinkedHashMap<>();
        }
    }

    public Map<String, Object> bindingsMap() {
        return CoaBindings.normalize(bindings);
    }

    public void setBindingsMap(Map<String, Object> map) {
        this.bindings = map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }
}
