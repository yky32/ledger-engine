package com.altech.ledger.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.support.DigestionRuleTestData;
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
class DigestionRuleRuntimeIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;

    @BeforeEach
    void seedRules() {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
    }

    @Test
    void runtimeFormulaChangeAffectsNextWebhookWithoutRedeploy() throws Exception {
        assertThat(digestionRuleRepository.count()).isGreaterThan(0);

        // Find PURCHASE rule
        MvcResult list = mockMvc.perform(get("/digestion-rules").param("enabledOnly", "true"))
            .andExpect(status().isOk())
            .andReturn();
        var arr = objectMapper.readTree(list.getResponse().getContentAsString()).get("data");
        Long purchaseId = null;
        for (var n : arr) {
            if ("PURCHASE".equalsIgnoreCase(n.get("eventType").asText())) {
                purchaseId = n.get("id").asLong();
                break;
            }
        }
        assertThat(purchaseId).isNotNull();

        // Set rate to 0.05 → 100 HKD = 5 LP
        mockMvc.perform(put("/digestion-rules/" + purchaseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formula\":{\"type\":\"RATE\",\"rate\":0.05}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.formula.type").value("RATE"))
            .andExpect(jsonPath("$.data.formula.rate").value(0.05));

        String cust = "DIG-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"x\"}"
                    .formatted(cust)))
            .andExpect(status().isOk());

        String eventId = "dig-evt-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, cust, "PURCHASE", new BigDecimal("100"), Currency.HKD, Instant.now(), Map.of());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andExpect(jsonPath("$.data.points").value(5.0));

        // Restore seed-like rate for other tests in same JVM
        mockMvc.perform(put("/digestion-rules/" + purchaseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formula\":{\"type\":\"RATE\",\"rate\":0.01}}"))
            .andExpect(status().isOk());
    }

    @Test
    void createAndDisableRule() throws Exception {
        String code = "T_CUSTOM_" + UUID.randomUUID().toString().substring(0, 6);
        MvcResult created = mockMvc.perform(post("/digestion-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code":"%s",
                      "name":"custom",
                      "eventType":"CUSTOM_EVT",
                      "operation":"EARN",
                      "isEnabled":true,
                      "priority":5,
                      "minAmount":0,
                      "formula":{"type":"FIXED","value":1},
                      "resultCurrency":"LP"
                    }
                    """.formatted(code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.code").value(code))
            .andExpect(jsonPath("$.data.formula.type").value("FIXED"))
            .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/digestion-rules/" + id + "/disable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isEnabled").value(false));

        mockMvc.perform(get("/digestion-rules").param("code", code))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isEnabled").value(false));
    }
}
