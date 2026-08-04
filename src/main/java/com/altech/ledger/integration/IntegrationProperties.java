package com.altech.ledger.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ledger.integration")
public class IntegrationProperties {
    private boolean enabled = true;
    private boolean programSetupEnabled = true;
    private List<String> programCurrencies = List.of("LP");
    private String walletRefTemplate = "wallet:{userId}:{currency}";
    private String expensePoolRefTemplate = "pool:loyalty-expense:{currency}";
    private String liabilityPoolRefTemplate = "pool:loyalty-liability:{currency}";
    private List<TransactionRule> rules = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isProgramSetupEnabled() { return programSetupEnabled; }
    public void setProgramSetupEnabled(boolean programSetupEnabled) { this.programSetupEnabled = programSetupEnabled; }
    public List<String> getProgramCurrencies() { return programCurrencies; }
    public void setProgramCurrencies(List<String> programCurrencies) { this.programCurrencies = programCurrencies; }
    public String getWalletRefTemplate() { return walletRefTemplate; }
    public void setWalletRefTemplate(String walletRefTemplate) { this.walletRefTemplate = walletRefTemplate; }
    public String getExpensePoolRefTemplate() { return expensePoolRefTemplate; }
    public void setExpensePoolRefTemplate(String expensePoolRefTemplate) {
        this.expensePoolRefTemplate = expensePoolRefTemplate;
    }
    public String getLiabilityPoolRefTemplate() { return liabilityPoolRefTemplate; }
    public void setLiabilityPoolRefTemplate(String liabilityPoolRefTemplate) {
        this.liabilityPoolRefTemplate = liabilityPoolRefTemplate;
    }
    public List<TransactionRule> getRules() { return rules; }
    public void setRules(List<TransactionRule> rules) { this.rules = rules; }

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

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public BigDecimal getMinAmount() { return minAmount; }
        public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
        public String getPointCurrency() { return pointCurrency; }
        public void setPointCurrency(String pointCurrency) { this.pointCurrency = pointCurrency; }
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
        public String getProcessType() { return processType; }
        public void setProcessType(String processType) { this.processType = processType; }
    }
}
