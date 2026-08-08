package com.altech.ledger.movement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MovementIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void depositCreditsWalletViaJournal() throws Exception {
        String owner = "DEP-" + UUID.randomUUID();
        onboard(owner, "Wallet A");

        String key = "dep-" + UUID.randomUUID();
        mockMvc.perform(post("/movements/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"movementKey":"%s","ownerId":"%s","currency":"LP","amount":250.00,"description":"Top up"}
                    """.formatted(key, owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"))
            .andExpect(jsonPath("$.data.orderType").value("DEPOSIT"));

        mockMvc.perform(get("/wallets/" + owner + "/LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.balance.ledgerBalance").value(250.00));
    }

    @Test
    void inWalletTransferMovesBalance() throws Exception {
        String ownerA = "XFER-A-" + UUID.randomUUID();
        String ownerB = "XFER-B-" + UUID.randomUUID();
        onboard(ownerA, "Wallet A");
        onboard(ownerB, "Wallet B");

        String depKey = "dep-" + UUID.randomUUID();
        mockMvc.perform(post("/movements/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"movementKey":"%s","ownerId":"%s","currency":"LP","amount":100.00}
                    """.formatted(depKey, ownerA)))
            .andExpect(status().isOk());

        String xferKey = "xfer-" + UUID.randomUUID();
        mockMvc.perform(post("/movements/transfers/in-wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"movementKey":"%s","fromOwnerId":"%s","toOwnerId":"%s","currency":"LP","amount":40.00}
                    """.formatted(xferKey, ownerA, ownerB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SETTLED"));

        mockMvc.perform(get("/wallets/" + ownerA + "/LP"))
            .andExpect(jsonPath("$.data.balance.ledgerBalance").value(60.00));
        mockMvc.perform(get("/wallets/" + ownerB + "/LP"))
            .andExpect(jsonPath("$.data.balance.ledgerBalance").value(40.00));
    }

    @Test
    void movementIsIdempotentByMovementKey() throws Exception {
        String owner = "DUP-" + UUID.randomUUID();
        onboard(owner, "Wallet");

        String key = "dep-dup-" + UUID.randomUUID();
        String body = """
            {"movementKey":"%s","ownerId":"%s","currency":"LP","amount":10.00}
            """.formatted(key, owner);

        mockMvc.perform(post("/movements/deposits").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        mockMvc.perform(post("/movements/deposits").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.movementKey").value(key));
    }

    private void onboard(String userId, String name) throws Exception {
        var result = mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"associatedIdentifier":"%s","currency":"LP","name":"%s"}
                    """.formatted(userId, name)))
            .andReturn();
        assertThat(result.getResponse().getStatus()).isIn(200, 409);
    }
}
