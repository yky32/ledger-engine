package com.altech.ledger.integration;

import com.altech.ledger.repository.IngestPolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestPolicyRuntimeIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired IngestPolicyRepository ingestPolicyRepository;

    @Test
    void getCreatesDefaultAndPutUpdatesRuntime() throws Exception {
        mockMvc.perform(get("/ingest-policies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isEnabled").value(true))
            .andExpect(jsonPath("$.data.isAutoCreateWallet").value(true))
            .andExpect(jsonPath("$.data.autoWalletSettlementCurrency").value("HKD"))
            .andExpect(jsonPath("$.data.autoWalletEnsureCurrency").value("LP"));

        assertThat(ingestPolicyRepository.count()).isGreaterThan(0);

        mockMvc.perform(put("/ingest-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isAutoCreateWallet\":false,\"autoWalletNamePrefix\":\"Lazy \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isAutoCreateWallet").value(false))
            .andExpect(jsonPath("$.data.autoWalletNamePrefix").value("Lazy "));

        mockMvc.perform(put("/ingest-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isAutoCreateWallet\":true,\"isEnabled\":true,\"autoWalletNamePrefix\":\"Auto \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isAutoCreateWallet").value(true));
    }
}
