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
 * Phase-1 wallet onboarding integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WalletOnboardingIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void singleOnboardCreatesActiveWalletAndIsQueryable() throws Exception {
        String extIdentifier = "ONB-" + UUID.randomUUID();

        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"extIdentifier":"%s","currency":"LP","name":"Alice"}
                    """.formatted(extIdentifier)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SYS0000"))
            .andExpect(jsonPath("$.data.ownerId").value(extIdentifier))
            .andExpect(jsonPath("$.data.extIdentifier").value(extIdentifier))
            .andExpect(jsonPath("$.data.currency").value("LP"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.walletId").isNumber())
            .andExpect(jsonPath("$.data.balance.ledgerBalance").value(0))
            .andExpect(jsonPath("$.data.account.externalReference").value(org.hamcrest.Matchers.matchesPattern("\\d+")))
            .andExpect(jsonPath("$.data.createDt").exists());

        mockMvc.perform(get("/wallets/" + extIdentifier + "/LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.ownerId").value(extIdentifier))
            .andExpect(jsonPath("$.data.extIdentifier").value(extIdentifier));

        mockMvc.perform(get("/wallets").param("ownerId", extIdentifier))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"extIdentifier":"%s","currency":"LP","name":"Alice again"}
                    """.formatted(extIdentifier)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("WAL0409"));
    }

    @Test
    void batchOnboardsFromCrmAndIsIdempotent() throws Exception {
        String a = "CRM-" + UUID.randomUUID();
        String b = "CRM-" + UUID.randomUUID();
        String body = """
            {
              "wallets": [
                {"extIdentifier":"%s","currency":"LP","name":"Alice"},
                {"extIdentifier":"%s","currency":"LP","name":"Bob"}
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
            .andExpect(jsonPath("$.data.alreadyExists").value(2))
            .andExpect(jsonPath("$.data.alreadyExistingExtIdentifiers").isArray())
            .andExpect(jsonPath("$.data.alreadyExistingExtIdentifiers.length()").value(2));
    }
}
