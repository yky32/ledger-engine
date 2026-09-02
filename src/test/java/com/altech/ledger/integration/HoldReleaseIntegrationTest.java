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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HoldReleaseIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;

    @BeforeEach
    void seed() {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
    }

    @Test
    void holdReducesAvailableOnlyThenReleaseRestores() throws Exception {
        String cust = "HD-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"h\"}"
                    .formatted(cust)))
            .andExpect(status().isOk());

        // earn 10 LP via purchase 1000 * 0.01
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventId":"hd-%s","ownerId":"%s","eventType":"PURCHASE",
                     "amount":1000,"currency":"HKD","occurredAt":"%s"}
                    """.formatted(UUID.randomUUID(), cust, java.time.Instant.now())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"));

        MvcResult before = mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk()).andReturn();
        JsonNode lp0 = _lp(before);
        var ledger0 = JsonMoney.bd(lp0.get("ledgerBalance"));
        var avail0 = JsonMoney.bd(lp0.get("availableBalance"));
        assertThat(avail0).isEqualByComparingTo(ledger0);

        mockMvc.perform(post("/wallets/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","currency":"LP","amount":3,"movementKey":"hold-%s"}
                    """.formatted(cust, UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderType").value("HOLD"));

        MvcResult held = mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk()).andReturn();
        JsonNode lp1 = _lp(held);
        assertThat(JsonMoney.bd(lp1.get("ledgerBalance"))).isEqualByComparingTo(ledger0);
        assertThat(JsonMoney.bd(lp1.get("availableBalance"))).isEqualByComparingTo(avail0.subtract(new java.math.BigDecimal("3")));

        mockMvc.perform(post("/wallets/releases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","currency":"LP","amount":3,"movementKey":"rel-%s"}
                    """.formatted(cust, UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderType").value("RELEASE"));

        MvcResult after = mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk()).andReturn();
        JsonNode lp2 = _lp(after);
        assertThat(JsonMoney.bd(lp2.get("ledgerBalance"))).isEqualByComparingTo(ledger0);
        assertThat(JsonMoney.bd(lp2.get("availableBalance"))).isEqualByComparingTo(avail0);
    }

    private JsonNode _lp(MvcResult r) throws Exception {
        JsonNode accounts = objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("accounts");
        for (JsonNode a : accounts) {
            if ("LP".equals(a.get("currency").asText())) {
                return a;
            }
        }
        throw new AssertionError("no LP account");
    }
}
