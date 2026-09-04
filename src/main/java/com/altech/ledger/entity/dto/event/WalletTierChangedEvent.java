package com.altech.ledger.entity.dto.event;

import com.altech.core.constant.enu.Currency;
import com.altech.core.kafka.BaseEvent;
import com.altech.ledger.entity.enu.OrderType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/** Outbound: wallet.tier actually moved. Topic default {@code ledger.wallet.tier-changed}. */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletTierChangedEvent extends BaseEvent {
    private Long walletId;
    private String ownerId;
    private String fromTier;
    private String toTier;
    private BigDecimal lpLedgerBalance;
    private Currency currency;
    private Long movementId;
    private String movementKey;
    private OrderType orderType;
    /** UPGRADE or DOWNGRADE. */
    private String reason;
}
