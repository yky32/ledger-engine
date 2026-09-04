package com.altech.ledger.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ledger.movement.kafka")
public class MovementKafkaProperties {
    private boolean enabled = false;
    private String initiatedTopic = "ledger.movement.initiated";
    private String doneTopic = "ledger.movement.done";
    /** Inbound: trigger execute (legacy name). */
    private String balanceUpdateTopic = "ledger.movement.balance-update";
    /** Outbound: after balances applied — consumers subscribe here. */
    private String balanceUpdatedTopic = "ledger.balance.updated";
    private String walletCreatedTopic = "ledger.wallet.created";
    private String walletTierChangedTopic = "ledger.wallet.tier-changed";
    private String groupId = "ledger-engine-movement";
}
