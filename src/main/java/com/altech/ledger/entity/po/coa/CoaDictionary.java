package com.altech.ledger.entity.po.coa;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.entity.enu.CoaDictionaryKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Operator-owned explanation of a COA segment or stem (01, 02, 01-02, 01-02-01).
 * Profiles ({@link CoaProfile}) are the live chart; this is the dictionary.
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_coa_dictionary_kind_code", columnNames = {"kind", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoaDictionary extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "coa_dictionary_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "coa_dictionary_id_generator")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoaDictionaryKind kind;

    /** Digit code or dashed stem: 01, 02, 01-02, 01-02-01. */
    @Column(nullable = false)
    private String code;

    @Column
    private String name;

    @Column(length = 2000)
    private String definition;

    @Column
    private String example;

    /** HOUSE | CUSTOMER | BOTH */
    @Builder.Default
    @Column
    private String side = "BOTH";

    /** Persist hook only trims / uppercases. Defaults live on {@code @Builder.Default}. */
    @PrePersist
    @PreUpdate
    void normalize() {
        if (code != null) {
            code = code.trim().replace(' ', '-');
        }
        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                name = null;
            }
        }
        if (definition != null) {
            definition = definition.trim();
            if (definition.isEmpty()) {
                definition = null;
            }
        }
        if (example != null) {
            example = example.trim();
            if (example.isEmpty()) {
                example = null;
            }
        }
        if (side != null) {
            side = side.trim().toUpperCase();
            if (side.isEmpty()) {
                side = "BOTH";
            }
        } else {
            side = "BOTH";
        }
    }
}
