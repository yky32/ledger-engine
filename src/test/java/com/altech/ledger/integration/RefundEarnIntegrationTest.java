package com.altech.ledger.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.JsonMoney;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.enu.MovementDirection;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RefundEarnIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;

    @BeforeEach
    void seed() {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
    }

    @Test
    void refundReversesEarnLegsAndBalances() throws Exception {
        String cust = "RF-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"rf\"}"
                    .formatted(cust)))
            .andExpect(status().isOk());

        String eventId = "rf-evt-" + UUID.randomUUID();
        TransactionalEvent event = new TransactionalEvent(
            eventId, cust, "PURCHASE", new BigDecimal("100"), Currency.HKD, Instant.now(), Map.of());

        MvcResult earn = mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andReturn();

        JsonNode earnData = objectMapper.readTree(earn.getResponse().getContentAsString()).get("data");
        long earnId = earnData.get("movementId").asLong();
        JsonNode earnLegs = earnData.get("legs");
        assertThat(earnLegs).hasSize(2);

        Map<String, String> origDirByBook = new HashMap<>();
        for (JsonNode leg : earnLegs) {
            origDirByBook.put(leg.get("fullNumber").asText(), leg.get("direction").asText());
        }

        MvcResult refund = mockMvc.perform(post("/movements/" + earnId + "/refund"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderType").value("ADJUSTMENT_REFUND"))
            .andExpect(jsonPath("$.data.status").value("SETTLED"))
            .andExpect(jsonPath("$.data.amount").value("-1"))
            .andReturn();

        JsonNode refundMv = objectMapper.readTree(refund.getResponse().getContentAsString()).get("data");
        long refundId = refundMv.get("id").asLong();
        assertThat(refundMv.get("associatedLedgerMovementId").asLong()).isEqualTo(earnId);

        MvcResult legsRes = mockMvc.perform(get("/integrations/ledger-entries")
                .param("movementId", String.valueOf(refundId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andReturn();
        JsonNode refundLegs = objectMapper.readTree(legsRes.getResponse().getContentAsString()).get("data");

        for (JsonNode leg : refundLegs) {
            String book = leg.get("fullNumber").asText();
            String dir = leg.get("direction").asText();
            String orig = origDirByBook.get(book);
            assertThat(orig).isNotNull();
            if ("CREDIT".equals(orig)) {
                assertThat(dir).isEqualTo(MovementDirection.DEBIT.name());
            } else {
                assertThat(dir).isEqualTo(MovementDirection.CREDIT.name());
            }
            assertThat(JsonMoney.bd(leg.get("amount"))).isEqualByComparingTo("1");
        }

        mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value("0"));

        mockMvc.perform(get("/wallets/" + cust + "/movements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.id==" + earnId + ")].status").value("REFUNDED"));

        MvcResult again = mockMvc.perform(post("/movements/" + earnId + "/refund"))
            .andExpect(status().isOk())
            .andReturn();
        assertThat(objectMapper.readTree(again.getResponse().getContentAsString())
            .get("data").get("id").asLong()).isEqualTo(refundId);
    }
}
