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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletTierIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;

    @BeforeEach
    void seed() {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
    }

    @Test
    void earnUpgradesAndRefundDowngrades() throws Exception {
        mockMvc.perform(put("/wallet-tier-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "isEnabled": true,
                      "criterion": "LEDGER_BALANCE",
                      "currency": "LP",
                      "bands": [
                        { "code": "NONE", "upgradeAt": "0" },
                        { "code": "SILVER", "upgradeAt": "1" }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isEnabled").value(true));

        String cust = "WT-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"wt\"}"
                    .formatted(cust)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tier").value("NONE"));

        String eventId = "wt-evt-" + UUID.randomUUID();
        TransactionalEvent earn = TransactionalEvent.of(
            eventId, cust, "PURCHASE", new BigDecimal("100"), Currency.HKD, Instant.now(), Map.of());
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(earn)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"));

        mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tier").value("SILVER"));

        String refundJson = """
            {
              "eventId": "%s",
              "ownerId": "%s",
              "eventType": "CC_TXN",
              "action": "REFUND",
              "originalEventId": "%s",
              "amount": "100.00",
              "currency": "HKD"
            }
            """.formatted("wt-ref-" + UUID.randomUUID(), cust, eventId);
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tier").value("NONE"));
    }

    @Test
    void policyGetSeedsDraftBands() throws Exception {
        mockMvc.perform(get("/wallet-tier-policies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.criterion").value("LEDGER_BALANCE"))
            .andExpect(jsonPath("$.data.currency").value("LP"))
            .andExpect(jsonPath("$.data.bands[0].code").value("NONE"));
    }

    @Test
    void earnDoesNotUpgradeUntilOpsEnables() throws Exception {
        mockMvc.perform(put("/wallet-tier-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isEnabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isEnabled").value(false));
        String cust = "WT0-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"wt0\"}"
                    .formatted(cust)))
            .andExpect(status().isOk());
        String eventId = "wt0-evt-" + UUID.randomUUID();
        TransactionalEvent earn = TransactionalEvent.of(
            eventId, cust, "PURCHASE", new BigDecimal("100"), Currency.HKD, Instant.now(), Map.of());
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(earn)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"));
        mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tier").value("NONE"));
    }
}
