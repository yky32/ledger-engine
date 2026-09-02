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

import java.util.UUID;

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
        String ownerA = "OWNER-A-" + UUID.randomUUID();
        String ownerB = "OWNER-B-" + UUID.randomUUID();

        MvcResult createA = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","settlementCurrency":"USD","name":"Wallet A"}
                    """.formatted(ownerA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andReturn();
        JsonNode walletA = objectMapper.readTree(createA.getResponse().getContentAsString());
        long walletAId = walletA.get("data").get("walletId").asLong();

        MvcResult createB = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","settlementCurrency":"USD","name":"Wallet B"}
                    """.formatted(ownerB)))
            .andExpect(status().isOk())
            .andReturn();
        long walletBId = objectMapper.readTree(createB.getResponse().getContentAsString())
            .get("data").get("walletId").asLong();

        mockMvc.perform(post("/ledger/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetWalletId":"%s","currency":"USD","amount":100.00,"mode":"AUTO","movementKey":"dep-parity-%s"}
                    """.formatted(walletAId, UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"))
            .andExpect(jsonPath("$.data.orderType").value("DEPOSIT"));

        mockMvc.perform(get("/wallets/" + ownerA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value("100.00"));

        mockMvc.perform(post("/ledger/wallet-transfers/in-wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fromWalletId":"%s","toWalletId":"%s","currency":"USD","amount":40.00,"mode":"AUTO","movementKey":"xfer-parity-%s"}
                    """.formatted(walletAId, walletBId, UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"));

        mockMvc.perform(get("/wallets/" + ownerA))
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value("60.00"));
        mockMvc.perform(get("/wallets/" + ownerB))
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value("40.00"));

        mockMvc.perform(post("/ledger/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"originatorWalletId":"%s","currency":"USD","amount":10.00,"mode":"AUTO","movementKey":"wd-parity-%s"}
                    """.formatted(walletAId, UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"));

        mockMvc.perform(get("/wallets/" + ownerA))
            .andExpect(jsonPath("$.data.accounts[0].ledgerBalance").value("50.00"));

        mockMvc.perform(get("/wallets/" + ownerA + "/movements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(post("/fx-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"base":"USD","target":"HKD","rate":7.8}
                    """))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 409));

        mockMvc.perform(post("/accounting-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"deposit-rule-%s","description":"test","direction":"CREDIT","multiplier":1}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isOk());

        mockMvc.perform(get("/dashboards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.walletCount").isNumber());

        assertThat(walletAId).isPositive();
    }
}
