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
 * Phase A: 1 CUST : 1 Wallet + DEFAULT AccountSet + HKD/LP CoA roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WalletOnboardingIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void singleOnboardCreatesDefaultCoaAndIsQueryable() throws Exception {
        String associatedIdentifier = "01A" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));

        // HKD: AVAILABLE,HELD,ADJUST (3) + LP: AVAILABLE,HELD,REDEEMED,EXPIRED,ADJUST (5) = 8
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"associatedIdentifier":"%s","settlementCurrency":"HKD","name":"Alice"}
                    """.formatted(associatedIdentifier)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SYS0000"))
            .andExpect(jsonPath("$.data.associatedIdentifier").value(associatedIdentifier))
            .andExpect(jsonPath("$.data.settlementCurrency").value("HKD"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.account.currency").value("HKD"))
            .andExpect(jsonPath("$.data.accounts.length()").value(8))
            .andExpect(jsonPath("$.data.accountSets.length()").value(1))
            .andExpect(jsonPath("$.data.accountSets[0].code").value("DEFAULT"))
            .andExpect(jsonPath("$.data.accountSets[0].accounts.length()").value(8));

        mockMvc.perform(get("/wallets/" + associatedIdentifier))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.associatedIdentifier").value(associatedIdentifier))
            .andExpect(jsonPath("$.data.accounts.length()").value(8))
            .andExpect(jsonPath("$.data.accountSets[0].code").value("DEFAULT"))
            .andExpect(jsonPath("$.data.accounts[?(@.currency=='HKD' && @.accountRole=='AVAILABLE')]").exists())
            .andExpect(jsonPath("$.data.accounts[?(@.currency=='LP' && @.accountRole=='AVAILABLE')]").exists())
            .andExpect(jsonPath("$.data.accounts[?(@.currency=='LP' && @.accountRole=='HELD')]").exists());

        // currencies filter — LP only (5 LP roles)
        mockMvc.perform(get("/wallets/" + associatedIdentifier).param("currencies", "LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accounts.length()").value(5))
            .andExpect(jsonPath("$.data.accounts[0].currency").value("LP"));

        // HKD only (3 roles)
        mockMvc.perform(get("/wallets/" + associatedIdentifier).param("currencies", "HKD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accounts.length()").value(3));

        // Upsert (idempotent) — 200 not 409
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"associatedIdentifier":"%s","settlementCurrency":"HKD","name":"Alice updated"}
                    """.formatted(associatedIdentifier)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.associatedIdentifier").value(associatedIdentifier));
    }

    @Test
    void batchOnboardsFromCrmAndIsIdempotent() throws Exception {
        String a = "01A" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        String b = "01A" + String.format("%08d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000));
        String body = """
            {
              "wallets": [
                {"associatedIdentifier":"%s","settlementCurrency":"HKD","name":"Alice"},
                {"associatedIdentifier":"%s","settlementCurrency":"HKD","name":"Bob"}
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
