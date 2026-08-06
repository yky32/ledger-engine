package com.altech.ledger.parity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LedgerWalletParityIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void walletDepositWithdrawTransferParityPaths() throws Exception {
        // create wallet A
        MvcResult createA = mockMvc.perform(post("/ledger-wallets/full")
                .param("ownerId", "OWNER-A")
                .param("currency", "USD")
                .param("extIdentifier", "tenant-a")
                .param("extType", "tenant"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
        JsonNode walletA = objectMapper.readTree(createA.getResponse().getContentAsString());
        long walletAId = walletA.get("id").asLong();

        // create wallet B
        MvcResult createB = mockMvc.perform(post("/ledger-wallets/full")
                .param("ownerId", "OWNER-B")
                .param("currency", "USD"))
            .andExpect(status().isCreated())
            .andReturn();
        long walletBId = objectMapper.readTree(createB.getResponse().getContentAsString()).get("id").asLong();

        // activate both
        mockMvc.perform(post("/ledger-wallets/" + walletAId + "/activations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post("/ledger-wallets/" + walletBId + "/activations"))
            .andExpect(status().isOk());

        // deposit
        mockMvc.perform(post("/ledger/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetWalletId":"%s","currency":"USD","amount":100.00,"mode":"AUTO","movementKey":"dep-parity-1"}
                    """.formatted(walletAId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SETTLED"))
            .andExpect(jsonPath("$.orderType").value("DEPOSIT"));

        mockMvc.perform(get("/ledger-wallets/" + walletAId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accounts[0].ledgerBalance").value(100.0));

        // transfer
        mockMvc.perform(post("/ledger/wallet-transfers/in-wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fromWalletId":"%s","toWalletId":"%s","currency":"USD","amount":40.00,"mode":"AUTO","movementKey":"xfer-parity-1"}
                    """.formatted(walletAId, walletBId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SETTLED"));

        mockMvc.perform(get("/ledger-wallets/" + walletAId))
            .andExpect(jsonPath("$.accounts[0].ledgerBalance").value(60.0));
        mockMvc.perform(get("/ledger-wallets/" + walletBId))
            .andExpect(jsonPath("$.accounts[0].ledgerBalance").value(40.0));

        // withdraw
        mockMvc.perform(post("/ledger/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"originatorWalletId":"%s","currency":"USD","amount":10.00,"mode":"AUTO","movementKey":"wd-parity-1"}
                    """.formatted(walletAId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SETTLED"));

        mockMvc.perform(get("/ledger-wallets/" + walletAId))
            .andExpect(jsonPath("$.accounts[0].ledgerBalance").value(50.0));

        // movements query
        mockMvc.perform(get("/ledger-accounts/movements/my-movements").param("ownerId", "OWNER-A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());

        // fx + rules + recipients smoke
        mockMvc.perform(post("/fx-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"base":"USD","target":"HKD","rate":7.8}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"deposit-rule","description":"test","direction":"CREDIT","multiplier":1}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/recipients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"transferChannel":"SWIFT","status":"ACTIVE","tenantId":1,"metadata":"{}"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/dashboards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.walletCount").isNumber());

        // VA application
        MvcResult vaApp = mockMvc.perform(post("/virtual-accounts/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"HONG_KONG","extIdentifier":"VA-1","extType":"tenant"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(vaApp.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(patch("/virtual-accounts/applications/" + appId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"APPROVED"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.virtualAccountId").isNumber());

        assertThat(walletAId).isPositive();
    }
}
