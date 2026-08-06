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
    private String walletCreatedTopic = "ledger.wallet.created";
    private String groupId = "ledger-engine-movement";
}
