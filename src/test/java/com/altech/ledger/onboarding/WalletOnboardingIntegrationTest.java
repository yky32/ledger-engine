package com.altech.ledger.onboarding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletOnboardingIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void batchOnboardsFromCrmAndIsIdempotent() throws Exception {
        String body = """
            {
              "wallets": [
                {"userId":"CRM-BATCH-1","currency":"LP","name":"Alice"},
                {"userId":"CRM-BATCH-2","currency":"LP","name":"Bob"}
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/wallets/batch").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(2))
            .andExpect(jsonPath("$.alreadyExists").value(0));

        mockMvc.perform(post("/api/v1/wallets/batch").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(0))
            .andExpect(jsonPath("$.alreadyExists").value(2));
    }
}
