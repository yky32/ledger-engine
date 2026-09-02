package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAsOfBalanceResponseDto {
    private String ownerId;
    private Long walletId;
    private Instant asOf;
    private List<AccountAsOf> accounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountAsOf {
        private Long accountId;
        private Currency currency;
        private BigDecimal ledgerBalance;
        private BigDecimal availableBalance;
    }
}
