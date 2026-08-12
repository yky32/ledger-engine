package com.altech.ledger.entity.po.configuration;

import com.altech.core.entity.AuditEntityWithIsActive;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

/**
 * Key/value config scoped by target + scope.
 * <p>
 * {@code value} is JSONB (e.g. key {@code user-register.otp} → object payload).
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uniqueTargetAndScope", columnNames = {"target", "scope"}))
@Getter
@Setter
public class SystemConfiguration extends AuditEntityWithIsActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String target;

    private String scope;

    /**
     * Free-form JSON value, e.g. OTP policy / feature flags.
     * Example name: {@code user-register.otp}
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object value;
}
