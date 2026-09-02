package com.altech.ledger.parity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeepParityIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired com.altech.ledger.repository.DigestionRuleRepository digestionRuleRepository;

    @org.junit.jupiter.api.BeforeEach
    void digestionRules() {
        com.altech.ledger.support.DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
    }

    @Test
    void manualMultipartDepositThenSettle() throws Exception {
        String owner = "MANUAL-OWNER-" + UUID.randomUUID();
        long walletId = onboard(owner, "USD");
        String movementKey = "manual-dep-" + UUID.randomUUID();

        MvcResult dep = mockMvc.perform(multipart("/ledger/deposits")
                .file(new MockMultipartFile("files", "slip.pdf", "application/pdf", "x".getBytes()))
                .param("targetWalletId", String.valueOf(walletId))
                .param("currency", "USD")
                .param("amount", "50.00")
                .param("movementKey", movementKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING_DOCS"))
            .andReturn();

        long movementId = objectMapper.readTree(dep.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(put("/movements/" + movementId + "/settle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"));

        mockMvc.perform(get("/wallets/" + owner))
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value("50.00"));
    }

    @Test
    void depositAcceptsLegacyTargetIdField() throws Exception {
        String owner = "LEGACY-TARGET-" + UUID.randomUUID();
        long walletId = onboard(owner, "USD");

        mockMvc.perform(post("/ledger/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetId":"%s","currency":"USD","amount":25.00,"mode":"AUTO","movementKey":"legacy-target-%s"}
                    """.formatted(walletId, UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"));

        mockMvc.perform(get("/wallets/" + owner))
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value("25.00"));
    }

    @Test
    void insufficientFundsOnTransfer() throws Exception {
        long a = onboard("LOW-A-" + UUID.randomUUID(), "USD");
        long b = onboard("LOW-B-" + UUID.randomUUID(), "USD");

        mockMvc.perform(post("/ledger/wallet-transfers/in-wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fromWalletId":"%s","toWalletId":"%s","currency":"USD","amount":10.00,"mode":"AUTO","movementKey":"xfer-fail-%s"}
                    """.formatted(a, b, UUID.randomUUID())))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void movementStatusFilter() throws Exception {
        String ownerId = "FILTER-" + UUID.randomUUID();
        long walletId = onboard(ownerId, "USD");
        mockMvc.perform(post("/ledger/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetWalletId":"%s","currency":"USD","amount":5,"mode":"AUTO","movementKey":"filter-dep-%s"}
                    """.formatted(walletId, UUID.randomUUID())))
            .andExpect(status().isOk());

        mockMvc.perform(get("/wallets/" + ownerId + "/movements")
                .param("status", "SETTLED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void earnCreatesLedgerMovementLog() throws Exception {
        String userId = "EARN-" + UUID.randomUUID();
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","settlementCurrency":"LP","name":"Earn User"}
                    """.formatted(userId)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventId":"earn-evt-%s","ownerId":"%s","eventType":"PURCHASE","amount":100,"currency":"HKD","occurredAt":"%s"}
                    """.formatted(UUID.randomUUID(), userId, java.time.Instant.now().toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"));
    }

    private long onboard(String ownerId, String currency) throws Exception {
        MvcResult create = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","settlementCurrency":"%s","name":"%s"}
                    """.formatted(ownerId, currency, ownerId)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(create.getResponse().getContentAsString())
            .get("data").get("walletId").asLong();
    }
}
