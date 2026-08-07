package com.altech.ledger.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({IntegrationProperties.class, MovementKafkaProperties.class})
public class IntegrationConfig {
}
