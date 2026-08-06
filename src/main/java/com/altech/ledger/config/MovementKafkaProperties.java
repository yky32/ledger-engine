package com.altech.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledger.movement.kafka")
public class MovementKafkaProperties {
    private boolean enabled = false;
    private String initiatedTopic = "ledger.movement.initiated";
    private String doneTopic = "ledger.movement.done";
    private String walletCreatedTopic = "ledger.wallet.created";
    private String recipientCreatedTopic = "ledger.recipient.created";
    private String groupId = "ledger-engine-movement";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getInitiatedTopic() { return initiatedTopic; }
    public void setInitiatedTopic(String initiatedTopic) { this.initiatedTopic = initiatedTopic; }
    public String getDoneTopic() { return doneTopic; }
    public void setDoneTopic(String doneTopic) { this.doneTopic = doneTopic; }
    public String getWalletCreatedTopic() { return walletCreatedTopic; }
    public void setWalletCreatedTopic(String walletCreatedTopic) { this.walletCreatedTopic = walletCreatedTopic; }
    public String getRecipientCreatedTopic() { return recipientCreatedTopic; }
    public void setRecipientCreatedTopic(String recipientCreatedTopic) { this.recipientCreatedTopic = recipientCreatedTopic; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
}
