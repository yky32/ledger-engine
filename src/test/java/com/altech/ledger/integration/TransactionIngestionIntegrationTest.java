package com.altech.ledger.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.repository.FailedTransactionIngestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionIngestionIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired FailedTransactionIngestRepository failedTransactionIngestRepository;
    @Autowired com.altech.ledger.repository.DigestionRuleRepository digestionRuleRepository;

    @BeforeEach
    void onboardWallets() throws Exception {
        com.altech.ledger.support.DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
        for (String id : new String[] { "CUST-9001", "CUST-9003" }) {
            ensureOnboarded(id);
        }
    }

    @Test
    void webhookEarnsLpWhenEligiblePurchase() throws Exception {
        String eventId = "evt-" + UUID.randomUUID();
        // HKD 200 * RATE 0.01 = 2 LP
        TransactionalEvent event = new TransactionalEvent(
            eventId, "CUST-9001", "PURCHASE", new BigDecimal("200.00"), Currency.HKD,
            Instant.now().minus(1, ChronoUnit.HOURS), Map.of("source", "pos"));

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andExpect(jsonPath("$.data.operation").value("EARN"))
            .andExpect(jsonPath("$.data.points").value("2"));
    }

    @Test
    void webhookAcceptsAssociatedIdentifierJsonKey() throws Exception {
        ensureOnboarded("CUST-9010");
        String body = """
            {
              "eventId": "evt-alias-%s",
              "ownerId": "CUST-9010",
              "eventType": "PURCHASE",
              "amount": 100,
              "currency": "HKD",
              "occurredAt": "%s"
            }
            """.formatted(UUID.randomUUID(), Instant.now().toString());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andExpect(jsonPath("$.data.points").value("1"));
    }

    @Test
    void webhookAutoCreatesWalletThenEarnsWhenNotOnboarded() throws Exception {
        String cust = "AUTO-" + UUID.randomUUID().toString().substring(0, 8);
        String eventId = "evt-auto-" + UUID.randomUUID();
        // HKD 100 * 0.01 = 1 LP — no prior onboard
        TransactionalEvent event = new TransactionalEvent(
            eventId, cust, "PURCHASE", new BigDecimal("100"), Currency.HKD,
            Instant.now(), Map.of());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andExpect(jsonPath("$.data.points").value("1"))
            .andExpect(jsonPath("$.data.walletExternalReference").value(cust));

        mockMvc.perform(get("/wallets/" + cust))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ownerId").value(cust))
            .andExpect(jsonPath("$.data.settlementCurrency").value("HKD"))
            .andExpect(jsonPath("$.data.accounts.length()").value(2))
            .andExpect(jsonPath("$.data.accounts[*].currency",
                org.hamcrest.Matchers.containsInAnyOrder("HKD", "LP")));
    }

    @Test
    void webhookSkipsIneligibleCurrencyAndPersists() throws Exception {
        ensureOnboarded("CUST-9002");
        String eventId = "evt-ccy-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, "CUST-9002", "PURCHASE", new BigDecimal("50"), Currency.JPY,
            Instant.now(), Map.of());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SKIPPED"));

        assertThat(failedTransactionIngestRepository.findByEventIdOrderByIdDesc(eventId).get(0).getFailureCode())
            .isEqualTo("CURRENCY");
    }

    @Test
    void webhookSkipsWhenOlderThanMaxAgeDays() throws Exception {
        ensureOnboarded("CUST-9004");
        String eventId = "evt-age-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, "CUST-9004", "PURCHASE", new BigDecimal("50"), Currency.HKD,
            Instant.now().minus(10, ChronoUnit.DAYS), Map.of());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SKIPPED"));

        assertThat(failedTransactionIngestRepository.findByEventIdOrderByIdDesc(eventId).get(0).getFailureCode())
            .isEqualTo("AGE");
    }

    @Test
    void webhookSkipsWhenOccurredAtMissingForMaxAgeRule() throws Exception {
        ensureOnboarded("CUST-9005");
        String eventId = "evt-no-ts-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, "CUST-9005", "PURCHASE", new BigDecimal("50"), Currency.HKD,
            null, Map.of());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SKIPPED"));

        assertThat(failedTransactionIngestRepository.findByEventIdOrderByIdDesc(eventId).get(0).getFailureCode())
            .isEqualTo("AGE");
    }

    @Test
    void webhookIsIdempotentForSameEventId() throws Exception {
        String eventId = "evt-dup-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, "CUST-9003", "SIGNUP", BigDecimal.ZERO, Currency.LP, null, Map.of());
        String body = objectMapper.writeValueAsString(event);

        mockMvc.perform(post("/integrations/webhooks/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andExpect(jsonPath("$.data.points").value("100"));

        mockMvc.perform(post("/integrations/webhooks/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DUPLICATE"));
    }

    /** settlement LP primary so earn/burn points hit LP account */
    private void ensureOnboarded(String ownerId) throws Exception {
        var result = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","settlementCurrency":"LP","name":"Test wallet"}
                    """.formatted(ownerId)))
            .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(200, 409);
    }
}
