package com.altech.ledger.config;

import com.altech.ledger.service.PaymentRailPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({IntegrationProperties.class, MovementKafkaProperties.class})
public class IntegrationConfig {

    @Bean
    public PaymentRailPort paymentRailPort() {
        return new PaymentRailPort.NoOpPaymentRailPort();
    }
}
