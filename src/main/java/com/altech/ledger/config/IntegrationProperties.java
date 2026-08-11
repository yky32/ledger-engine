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
    private String walletRefTemplate = "wallet:{associatedIdentifier}:{currency}";
    private List<TransactionRule> rules = new ArrayList<>();

    /**
     * When true: after eligibility gates, missing wallet is auto-created then earn/burn continues
     * in the same request/transaction.
     * <p>
     * YAML: {@code ledger.integration.auto-create-wallet} / env {@code LEDGER_AUTO_CREATE_WALLET}.
     */
    private boolean isAutoCreateWallet = true;

    /** Defaults used when auto-creating a wallet from webhook. */
    private AutoWallet autoWallet = new AutoWallet();

    @Getter
    @Setter
    public static class AutoWallet {
        /** Settlement + primary account currency. Default HKD. */
        private String settlementCurrency = "HKD";
        /** Always open this book under the wallet (earn points). Default LP. */
        private String ensureCurrency = "LP";
        private String associatedFrom = "CRM";
        private String namePrefix = "Auto ";
    }

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

        /**
         * Eligible transaction currencies (ISO codes). Empty = no currency filter.
         * Example: HKD, USD
         */
        private List<String> eligibleCurrencies = new ArrayList<>();

        /**
         * Max age of {@code occurredAt} in days. Null = no age check.
         * When set, missing {@code occurredAt} → skip.
         */
        private Integer maxAgeDays;
    }
}
