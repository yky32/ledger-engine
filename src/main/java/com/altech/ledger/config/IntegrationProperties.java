package com.altech.ledger.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integration runtime flags (not digestion rule catalog — rules live in DB via /digestion-rules).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ledger.integration")
public class IntegrationProperties {
    private boolean enabled = true;
    private String walletRefTemplate = "wallet:{associatedIdentifier}:{currency}";

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
}
