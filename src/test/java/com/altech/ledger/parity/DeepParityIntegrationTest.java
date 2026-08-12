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
        long walletId = createAndActivate("MANUAL-OWNER", "USD");
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

        mockMvc.perform(put("/ledger-accounts/movements/" + movementId + "/settle"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"));

        mockMvc.perform(get("/ledger-wallets/" + walletId))
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value(50.0));
    }

    @Test
    void depositAcceptsLegacyTargetIdField() throws Exception {
        long walletId = createAndActivate("LEGACY-TARGET", "USD");

        mockMvc.perform(post("/ledger/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetId":"%s","currency":"USD","amount":25.00,"mode":"AUTO","movementKey":"legacy-target-%s"}
                    """.formatted(walletId, UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"));

        mockMvc.perform(get("/ledger-wallets/" + walletId))
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value(25.0));
    }

    @Test
    void insufficientFundsOnTransfer() throws Exception {
        long a = createAndActivate("LOW-A", "USD");
        long b = createAndActivate("LOW-B", "USD");

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
        long walletId = createAndActivateFixed(ownerId, "USD");
        mockMvc.perform(post("/ledger/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetWalletId":"%s","currency":"USD","amount":5,"mode":"AUTO","movementKey":"filter-dep-%s"}
                    """.formatted(walletId, UUID.randomUUID())))
            .andExpect(status().isOk());

        mockMvc.perform(get("/ledger-accounts/movements/my-movements")
                .param("ownerId", ownerId)
                .param("statuses", "SETTLED"))
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

    private long createAndActivate(String ownerPrefix, String currency) throws Exception {
        return createAndActivateFixed(ownerPrefix + "-" + UUID.randomUUID(), currency);
    }

    private long createAndActivateFixed(String ownerId, String currency) throws Exception {
        MvcResult create = mockMvc.perform(post("/ledger-wallets/full")
                .param("ownerId", ownerId)
                .param("currency", currency))
            .andExpect(status().isOk())
            .andReturn();
        long id = objectMapper.readTree(create.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/ledger-wallets/" + id + "/activations"))
            .andExpect(status().isOk());
        return id;
    }
}
