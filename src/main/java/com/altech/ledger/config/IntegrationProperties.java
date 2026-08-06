package com.altech.ledger.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "ledger.integration")
public class IntegrationProperties {
    private boolean enabled = true;
    private boolean programSetupEnabled = true;
    private List<String> programCurrencies = List.of("LP");
    private String walletRefTemplate = "wallet:{userId}:{currency}";
    private String expensePoolRefTemplate = "pool:loyalty-expense:{currency}";
    private String liabilityPoolRefTemplate = "pool:loyalty-liability:{currency}";
    private String depositClearingRefTemplate = "pool:clearing-deposit:{currency}";
    private String withdrawalClearingRefTemplate = "pool:clearing-withdrawal:{currency}";
    private List<TransactionRule> rules = new ArrayList<>();

    @Getter
    @Setter
    public static class TransactionRule {
        private String eventType;
        /** EARN | BURN | PROCESS */
        private String operation = "EARN";
        private BigDecimal minAmount = BigDecimal.ZERO;
        private String pointCurrency = "LP";
        /** AMOUNT | FIXED:{n} | RATE:{n} */
        private String formula = "AMOUNT";
        /** HOLD | RELEASE | EXPIRE | ADJUST | TRANSFER | SETTLE — for PROCESS only */
        private String processType;
    }
}
