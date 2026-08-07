package com.altech.ledger.integration;

import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionIngestionIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void onboardWallets() throws Exception {
        for (String userId : new String[] { "CUST-9001", "CUST-9003" }) {
            ensureOnboarded(userId);
        }
    }

    @Test
    void webhookEarnsPointsWhenWalletOnboardedAndRuleMatches() throws Exception {
        String eventId = "evt-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, "CUST-9001", "PURCHASE", new BigDecimal("125.50"), "LP", null, Map.of());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andExpect(jsonPath("$.data.operation").value("EARN"))
            .andExpect(jsonPath("$.data.points").value(125.50));
    }

    @Test
    void webhookSkipsWhenWalletNotOnboarded() throws Exception {
        TransactionalEvent event = new TransactionalEvent(
            "evt-no-wallet-" + UUID.randomUUID(), "CUST-9999", "PURCHASE", new BigDecimal("50"), "LP", null, Map.of());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SKIPPED"))
            .andExpect(jsonPath("$.data.reason").exists());
    }

    @Test
    void webhookSkipsWhenBelowMinimum() throws Exception {
        ensureOnboarded("CUST-9002");
        TransactionalEvent event = new TransactionalEvent(
            "evt-low-" + UUID.randomUUID(), "CUST-9002", "PURCHASE", new BigDecimal("5"), "LP", null, Map.of());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SKIPPED"));
    }

    @Test
    void webhookIsIdempotentForSameEventId() throws Exception {
        String eventId = "evt-dup-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, "CUST-9003", "SIGNUP", BigDecimal.ZERO, "LP", null, Map.of());
        String body = objectMapper.writeValueAsString(event);

        mockMvc.perform(post("/integrations/webhooks/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andExpect(jsonPath("$.data.points").value(100));

        mockMvc.perform(post("/integrations/webhooks/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DUPLICATE"));
    }

    private void ensureOnboarded(String userId) throws Exception {
        var result = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"%s","currency":"LP","name":"Test wallet"}
                    """.formatted(userId)))
            .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(200, 409);
    }
}
