package com.altech.ledger.integration;

import com.altech.ledger.JsonMoney;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.support.DigestionRuleTestData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplyToIngestIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;

    @BeforeEach
    void seed() {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
    }

    @Test
    void ledgerThenAvailableThenVoidLedgerOnly() throws Exception {
        String cust = "AT-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"at\"}"
                    .formatted(cust)))
            .andExpect(status().isOk());

        String authId = "auth-" + UUID.randomUUID();
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventId":"%s","ownerId":"%s","eventType":"PURCHASE",
                     "amount":1000,"currency":"HKD","occurredAt":"%s","applyTo":"LEDGER"}
                    """.formatted(authId, cust, Instant.now())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"));

        JsonNode afterAuth = _lp(cust);
        assertThat(JsonMoney.bd(afterAuth.get("ledgerBalance"))).isEqualByComparingTo("10");
        assertThat(JsonMoney.bd(afterAuth.get("availableBalance"))).isEqualByComparingTo("0");

        String postId = "post-" + UUID.randomUUID();
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventId":"%s","ownerId":"%s","eventType":"PURCHASE",
                     "amount":1000,"currency":"HKD","occurredAt":"%s","applyTo":"AVAILABLE"}
                    """.formatted(postId, cust, Instant.now())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"));

        JsonNode afterPost = _lp(cust);
        assertThat(JsonMoney.bd(afterPost.get("ledgerBalance"))).isEqualByComparingTo("10");
        assertThat(JsonMoney.bd(afterPost.get("availableBalance"))).isEqualByComparingTo("10");

        String cust2 = "AV-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"av\"}"
                    .formatted(cust2)))
            .andExpect(status().isOk());
        String onlyAuth = "auth2-" + UUID.randomUUID();
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventId":"%s","ownerId":"%s","eventType":"PURCHASE",
                     "amount":1000,"currency":"HKD","occurredAt":"%s","applyTo":"LEDGER"}
                    """.formatted(onlyAuth, cust2, Instant.now())))
            .andExpect(status().isOk());
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventId":"%s","ownerId":"%s","eventType":"PURCHASE","action":"VOID",
                     "originalEventId":"%s","amount":1000,"currency":"HKD"}
                    """.formatted("void-" + UUID.randomUUID(), cust2, onlyAuth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REFUNDED"));
        JsonNode afterVoid = _lp(cust2);
        assertThat(JsonMoney.bd(afterVoid.get("ledgerBalance"))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(JsonMoney.bd(afterVoid.get("availableBalance"))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private JsonNode _lp(String ownerId) throws Exception {
        MvcResult r = mockMvc.perform(get("/wallets/" + ownerId).param("currencies", "LP"))
            .andExpect(status().isOk()).andReturn();
        JsonNode accounts = objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("accounts");
        for (JsonNode a : accounts) {
            if ("LP".equals(a.get("currency").asText())) {
                return a;
            }
        }
        throw new AssertionError("no LP account");
    }
}
