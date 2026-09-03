package com.altech.ledger.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.JsonMoney;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.repository.LedgerEntryRepository;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DoubleEntryEarnIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;

    @BeforeEach
    void seed() {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
    }

    @Test
    void earnProducesBalancedDebitAndCreditLegs() throws Exception {
        String cust = "DE-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"de\"}"
                    .formatted(cust)))
            .andExpect(status().isOk());

        String eventId = "de-evt-" + UUID.randomUUID();
        TransactionalEvent event = TransactionalEvent.of(
            eventId, cust, "PURCHASE", new BigDecimal("100"), Currency.HKD, Instant.now(), Map.of());

        MvcResult res = mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andExpect(jsonPath("$.data.points").value("1"))
            .andExpect(jsonPath("$.data.movementId").isNumber())
            .andExpect(jsonPath("$.data.legs").isArray())
            .andExpect(jsonPath("$.data.legs.length()").value(2))
            .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).get("data");
        long movementId = data.get("movementId").asLong();
        JsonNode legs = data.get("legs");
        assertThat(legs).hasSize(2);

        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (JsonNode leg : legs) {
            BigDecimal amt = JsonMoney.bd(leg.get("amount"));
            String dir = leg.get("direction").asText();
            if ("DEBIT".equals(dir)) {
                debit = debit.add(amt);
            } else if ("CREDIT".equals(dir)) {
                credit = credit.add(amt);
            }
        }
        assertThat(debit).isEqualByComparingTo(credit);
        assertThat(debit).isEqualByComparingTo("1");
        assertThat(legs.get(0).get("currency").asText()).isEqualTo(legs.get(1).get("currency").asText());
        assertThat(legs.get(0).get("currency").asText()).isEqualTo("LP");

        // Query API by eventId / movementId
        mockMvc.perform(get("/integrations/ledger-entries").param("eventId", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/integrations/ledger-entries").param("movementId", String.valueOf(movementId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/wallets/" + cust + "/movements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].currency").value("LP"));

        assertThat(ledgerEntryRepository.findByTxnId(movementId)).hasSize(2);
        assertThat(ledgerEntryRepository.findByTxnId(movementId).stream()
            .map(e -> e.getDirection())
            .toList())
            .containsExactlyInAnyOrder(MovementDirection.DEBIT, MovementDirection.CREDIT);
    }
}
