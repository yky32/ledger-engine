package com.altech.ledger.parity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeepParityIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void manualMultipartDepositThenSettle() throws Exception {
        long walletId = createAndActivate("MANUAL-OWNER", "USD");

        MvcResult dep = mockMvc.perform(multipart("/ledger/deposits")
                .file(new MockMultipartFile("files", "slip.pdf", "application/pdf", "x".getBytes()))
                .param("targetWalletId", String.valueOf(walletId))
                .param("currency", "USD")
                .param("amount", "50.00")
                .param("movementKey", "manual-dep-1"))
            .andExpect(status().isCreated())
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
                    {"targetId":"%s","currency":"USD","amount":25.00,"mode":"AUTO","movementKey":"legacy-target-1"}
                    """.formatted(walletId)))
            .andExpect(status().isCreated())
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
                    {"fromWalletId":"%s","toWalletId":"%s","currency":"USD","amount":10.00,"mode":"AUTO","movementKey":"xfer-fail-1"}
                    """.formatted(a, b)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void movementStatusFilter() throws Exception {
        long walletId = createAndActivate("FILTER-OWNER", "USD");
        mockMvc.perform(post("/ledger/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetWalletId":"%s","currency":"USD","amount":5,"mode":"AUTO","movementKey":"filter-dep-1"}
                    """.formatted(walletId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/ledger-accounts/movements/my-movements")
                .param("ownerId", "FILTER-OWNER")
                .param("statuses", "SETTLED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void earnCreatesLedgerMovementLog() throws Exception {
        // onboard via loyalty path (creates wallet:account ref without ledger-wallet necessarily)
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"EARN-USER-1","currency":"LP","name":"Earn User"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventId":"earn-evt-1","userId":"EARN-USER-1","eventType":"PURCHASE","amount":100,"currency":"LP"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"));
    }

    private long createAndActivate(String ownerId, String currency) throws Exception {
        MvcResult create = mockMvc.perform(post("/ledger-wallets/full")
                .param("ownerId", ownerId)
                .param("currency", currency))
            .andExpect(status().isCreated())
            .andReturn();
        long id = objectMapper.readTree(create.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(post("/ledger-wallets/" + id + "/activations"))
            .andExpect(status().isOk());
        return id;
    }
}
