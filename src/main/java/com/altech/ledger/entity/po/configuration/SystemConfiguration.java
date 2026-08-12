package com.altech.ledger.entity.po.configuration;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

/**
 * Mirrors tgt.program-management-service SystemConfiguration PO shape.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uniqueTargetAndScope", columnNames = {"target", "scope"})
})
@Builder
public class SystemConfiguration extends AuditEntityWithIsActive {

    @Id
    @GenericGenerator(name = "system_configuration_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "system_configuration_id_generator")
    private Long id;

    private String name; // user-register.otp

    private String target; // otp

    private String scope; // otp.global

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object value; // user-register.otp
}
