package com.altech.ledger.entity.dto.event;

import com.altech.core.constant.enu.Currency;
import com.altech.core.kafka.BaseEvent;
import com.altech.ledger.entity.enu.OrderType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Outbound Kafka event after balances are applied (SETTLED movement).
 * Topic default: {@code ledger.balance.updated}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceUpdatedEvent extends BaseEvent {

    private Long movementId;
    private String movementKey;
    private Long walletId;
    private String ownerId;
    private OrderType orderType;
    private BigDecimal amount;
    private Currency currency;
    private String description;

    @Builder.Default
    private List<AccountBalanceSnapshot> accounts = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AccountBalanceSnapshot {
        private Long accountId;
        private String fullNumber;
        private Currency currency;
        private BigDecimal ledgerBalance;
        private BigDecimal availableBalance;
        private boolean allowNegative;
    }
}
