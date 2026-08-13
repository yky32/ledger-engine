package com.altech.ledger.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.repository.FailedTransactionIngestRepository;
import com.altech.ledger.support.DigestionRuleTestData;
import com.altech.ledger.usecase.ingest.IngestPolicyUseCase;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FailedIngestReplayIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;
    @Autowired FailedTransactionIngestRepository failedRepo;
    @Autowired IngestPolicyUseCase ingestPolicyUseCase;

    @BeforeEach
    void seed() throws Exception {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
        ingestPolicyUseCase.getOrCreate();
        mockMvc.perform(put("/ingest-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isEnabled\":true,\"isAutoCreateWallet\":true}"))
            .andExpect(status().isOk());
    }

    @Test
    void replayAfterFixingCurrencyRuleEarns() throws Exception {
        String cust = "RP-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"r\"}"
                    .formatted(cust)))
            .andExpect(status().isOk());

        // JPY skipped by PURCHASE rule
        String eventId = "rp-evt-" + UUID.randomUUID();
        TransactionalEvent bad = new TransactionalEvent(
            eventId, cust, "PURCHASE", new BigDecimal("100"), Currency.JPY, Instant.now(), Map.of());
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SKIPPED"));

        MvcResult list = mockMvc.perform(get("/integrations/failed-transactions")
                .param("eventId", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("OPEN"))
            .andReturn();
        long failId = objectMapper.readTree(list.getResponse().getContentAsString())
            .get("data").get(0).get("id").asLong();

        // Expand eligible currencies to include JPY on PURCHASE rule
        MvcResult rules = mockMvc.perform(get("/digestion-rules").param("enabledOnly", "true"))
            .andExpect(status().isOk()).andReturn();
        JsonNode arr = objectMapper.readTree(rules.getResponse().getContentAsString()).get("data");
        Long purchaseId = null;
        for (JsonNode n : arr) {
            if ("PURCHASE".equalsIgnoreCase(n.get("eventType").asText())) {
                purchaseId = n.get("id").asLong();
                break;
            }
        }
        assertThat(purchaseId).isNotNull();
        mockMvc.perform(put("/digestion-rules/" + purchaseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eligibleCurrencies\":[\"HKD\",\"USD\",\"JPY\"]}"))
            .andExpect(status().isOk());

        // Replay → EARNED + REPLAYED
        mockMvc.perform(post("/integrations/failed-transactions/" + failId + "/replay"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REPLAYED"))
            .andExpect(jsonPath("$.data.ingestion.status").value("EARNED"))
            .andExpect(jsonPath("$.data.ingestion.legs").isArray());

        mockMvc.perform(get("/integrations/failed-transactions/" + failId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REPLAYED"));

        // restore HKD,USD only for other tests
        mockMvc.perform(put("/digestion-rules/" + purchaseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eligibleCurrencies\":[\"HKD\",\"USD\"]}"))
            .andExpect(status().isOk());
    }
}
