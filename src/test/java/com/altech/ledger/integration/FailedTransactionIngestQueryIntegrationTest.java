package com.altech.ledger.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FailedTransactionIngestQueryIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void listFailedAfterIneligibleCurrency() throws Exception {
        String cust = "FAILAPI-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"associatedIdentifier":"%s","settlementCurrency":"LP","name":"x"}
                    """.formatted(cust)))
            .andExpect(status().isOk());

        String eventId = "fail-api-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, cust, "PURCHASE", new BigDecimal("50"), Currency.JPY, Instant.now(), Map.of());
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SKIPPED"));

        mockMvc.perform(get("/integrations/failed-transactions")
                .param("associatedIdentifier", cust)
                .param("failureCode", "CURRENCY")
                .param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SYS0000"))
            .andExpect(jsonPath("$.data[0].eventId").value(eventId))
            .andExpect(jsonPath("$.data[0].failureCode").value("CURRENCY"))
            .andExpect(jsonPath("$.data[0].status").value("OPEN"));

        mockMvc.perform(get("/integrations/failed-transactions/by-event/" + eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].associatedIdentifier").value(cust));
    }
}
