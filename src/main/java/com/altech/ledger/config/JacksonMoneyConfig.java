package com.altech.ledger.config;

import com.altech.core.json.MoneyAmountModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Response money amounts as currency-scaled JSON strings. */
@Configuration
public class JacksonMoneyConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer moneyAmounts() {
        return builder -> builder.modulesToInstall(new MoneyAmountModule());
    }
}
