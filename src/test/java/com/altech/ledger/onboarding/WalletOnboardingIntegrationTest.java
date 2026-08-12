package com.altech.ledger.onboarding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase-1: 1 CUST : 1 Wallet + HKD primary + LP account; query by ownerId.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WalletOnboardingIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void singleOnboardCreatesActiveWalletAndIsQueryable() throws Exception {
        String ownerId = "01A" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));

        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","settlementCurrency":"HKD","name":"Alice",
                     "accounts":[{"currency":"LP","name":"Loyalty points","refCode":"LP"}]}
                    """.formatted(ownerId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SYS0000"))
            .andExpect(jsonPath("$.data.ownerId").value(ownerId))
            .andExpect(jsonPath("$.data.settlementCurrency").value("HKD"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.account.currency").value("HKD"))
            .andExpect(jsonPath("$.data.accounts.length()").value(2));

        // Query by same ownerId — full Wallet:Accounts
        mockMvc.perform(get("/wallets/" + ownerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ownerId").value(ownerId))
            .andExpect(jsonPath("$.data.settlementCurrency").value("HKD"))
            .andExpect(jsonPath("$.data.accounts.length()").value(2))
            .andExpect(jsonPath("$.data.accounts[?(@.currency=='HKD' && @.primary==true)]").exists())
            .andExpect(jsonPath("$.data.accounts[?(@.currency=='LP')]").exists());

        mockMvc.perform(get("/wallets").param("ownerId", ownerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.ownerId").value(ownerId))
            .andExpect(jsonPath("$.data.accounts.length()").value(2));

        // currencies filter — LP only
        mockMvc.perform(get("/wallets/" + ownerId).param("currencies", "LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accounts.length()").value(1))
            .andExpect(jsonPath("$.data.accounts[0].currency").value("LP"));

        // currencies filter — HKD,LP (order preserved by wallet sort, both present)
        mockMvc.perform(get("/wallets").param("ownerId", ownerId)
                .param("currencies", "HKD, LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accounts.length()").value(2));

        // Duplicate CUST → conflict
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","settlementCurrency":"HKD","name":"Alice again"}
                    """.formatted(ownerId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("WAL0409"));
    }

    @Test
    void batchOnboardsFromCrmAndIsIdempotent() throws Exception {
        String a = "01A" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        String b = "01A" + String.format("%08d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000));
        String body = """
            {
              "wallets": [
                {"ownerId":"%s","settlementCurrency":"HKD","name":"Alice",
                 "accounts":[{"currency":"LP","refCode":"LP"}]},
                {"ownerId":"%s","settlementCurrency":"HKD","name":"Bob",
                 "accounts":[{"currency":"LP","refCode":"LP"}]}
              ]
            }
            """.formatted(a, b);

        mockMvc.perform(post("/wallets/batch").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.created").value(2))
            .andExpect(jsonPath("$.data.alreadyExists").value(0));

        mockMvc.perform(post("/wallets/batch").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.created").value(0))
            .andExpect(jsonPath("$.data.alreadyExists").value(2));
    }
}
