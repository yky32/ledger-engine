package com.altech.ledger.entity.po.ledger;

import com.altech.ledger.entity.base.WalletIdAware;
import com.altech.ledger.entity.enu.PaymentMethodStatus;
import com.altech.ledger.entity.enu.PaymentMethodType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Port of the-wallet-ledger {@code PaymentMethod}.
 * Metadata stored as TEXT (json) for standalone deploy without jsonb type deps.
 */
@Entity
@Table(
    name = "payment_method",
    indexes = @Index(name = "payment_method_idx_walletId", columnList = "wallet_id")
)
public class PaymentMethod extends WalletIdAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethodType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentMethodStatus status;

    /** JSON blob — bank / card metadata (legacy PaymentMethodMetadata). */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "tokenization_value", length = 500)
    private String tokenizationValue;

    @Column(name = "tokenization_provider", length = 100)
    private String tokenizationProvider;

    @Column(length = 64)
    private String hash;

    protected PaymentMethod() {}

    public PaymentMethod(Long walletId, PaymentMethodType type, PaymentMethodStatus status, String metadata) {
        setWalletId(walletId);
        this.type = type;
        this.status = status;
        this.metadata = metadata;
        this.hash = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    @PrePersist
    private void ensureHash() {
        if (hash == null) {
            hash = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        }
    }

    public Long getId() { return id; }
    public PaymentMethodType getType() { return type; }
    public void setType(PaymentMethodType type) { this.type = type; }
    public PaymentMethodStatus getStatus() { return status; }
    public void setStatus(PaymentMethodStatus status) { this.status = status; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public String getTokenizationValue() { return tokenizationValue; }
    public void setTokenizationValue(String tokenizationValue) { this.tokenizationValue = tokenizationValue; }
    public String getTokenizationProvider() { return tokenizationProvider; }
    public void setTokenizationProvider(String tokenizationProvider) { this.tokenizationProvider = tokenizationProvider; }
    public String getHash() { return hash; }
}
