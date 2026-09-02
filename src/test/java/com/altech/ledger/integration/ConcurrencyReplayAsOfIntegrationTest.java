package com.altech.ledger.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.JsonMoney;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.repository.FailedTransactionIngestRepository;
import com.altech.ledger.support.DigestionRuleTestData;
import com.altech.ledger.usecase.ingest.IngestPolicyUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConcurrencyReplayAsOfIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;
    @Autowired FailedTransactionIngestRepository failedRepo;
    @Autowired IngestPolicyUseCase ingestPolicyUseCase;

    @BeforeEach
    void seed() throws Exception {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
        ingestPolicyUseCase.getOrCreate();
        mockMvc.perform(put("/ingest-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isEnabled\":true,\"isAutoCreateWallet\":true}"))
            .andExpect(status().isOk());
    }

    @Test
    void concurrentHoldsOnlyOneSucceedsWhenTightAvailable() throws Exception {
        String cust = "CX-" + UUID.randomUUID().toString().substring(0, 8);
        _onboardLp(cust);
        _earn(cust, "1000"); // 10 LP

        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        Future<?> f1 = pool.submit(() -> _holdRace(cust, "6", gate, ok, fail));
        Future<?> f2 = pool.submit(() -> _holdRace(cust, "6", gate, ok, fail));
        gate.countDown();
        f1.get();
        f2.get();
        pool.shutdownNow();

        assertThat(ok.get()).isEqualTo(1);
        assertThat(fail.get()).isEqualTo(1);

        MvcResult w = mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk()).andReturn();
        JsonNode lp = _lp(w);
        assertThat(JsonMoney.bd(lp.get("ledgerBalance"))).isEqualByComparingTo("10");
        assertThat(JsonMoney.bd(lp.get("availableBalance"))).isEqualByComparingTo("4");
    }

    @Test
    void holdIdempotentByMovementKey() throws Exception {
        String cust = "CI-" + UUID.randomUUID().toString().substring(0, 8);
        _onboardLp(cust);
        _earn(cust, "500"); // 5 LP
        String key = "hold-idem-" + UUID.randomUUID();
        String body = """
            {"ownerId":"%s","currency":"LP","amount":2,"movementKey":"%s"}
            """.formatted(cust, key);
        mockMvc.perform(post("/wallets/holds").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        mockMvc.perform(post("/wallets/holds").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        JsonNode lp = _lp(mockMvc.perform(get("/wallets/" + cust).param("currencies", "LP"))
            .andExpect(status().isOk()).andReturn());
        assertThat(JsonMoney.bd(lp.get("availableBalance"))).isEqualByComparingTo("3");
    }

    @Test
    void replayStillSkippedDoesNotDuplicateFailRow() throws Exception {
        String cust = "RF-" + UUID.randomUUID().toString().substring(0, 8);
        _onboardLp(cust);
        String eventId = "rf-" + UUID.randomUUID();
        TransactionalEvent bad = new TransactionalEvent(
            eventId, cust, "PURCHASE", new BigDecimal("10"), Currency.JPY, Instant.now(), Map.of());
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SKIPPED"));

        long before = failedRepo.findByEventIdOrderByIdDesc(eventId).size();
        assertThat(before).isEqualTo(1);
        long failId = failedRepo.findByEventIdOrderByIdDesc(eventId).get(0).getId();

        mockMvc.perform(post("/integrations/failed-transactions/" + failId + "/replay"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("OPEN"));

        assertThat(failedRepo.findByEventIdOrderByIdDesc(eventId)).hasSize(1);
    }

    @Test
    void historyAndAsOfAfterEarnAndHold() throws Exception {
        String cust = "HA-" + UUID.randomUUID().toString().substring(0, 8);
        _onboardLp(cust);
        _earn(cust, "1000"); // 10 LP
        mockMvc.perform(post("/wallets/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerId":"%s","currency":"LP","amount":4,"movementKey":"h-%s"}
                    """.formatted(cust, UUID.randomUUID())))
            .andExpect(status().isOk());

        mockMvc.perform(get("/wallets/" + cust + "/movements").param("orderType", "HOLD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.pagination.currentPage").value(1));

        MvcResult all = mockMvc.perform(get("/wallets/" + cust + "/movements").param("currency", "LP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();
        int te = objectMapper.readTree(all.getResponse().getContentAsString()).get("data").size();
        assertThat(te).isGreaterThanOrEqualTo(2);

        MvcResult asOf = mockMvc.perform(get("/wallets/" + cust + "/balances/as-of")
                .param("currency", "LP"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode acc = objectMapper.readTree(asOf.getResponse().getContentAsString())
            .get("data").get("accounts").get(0);
        assertThat(JsonMoney.bd(acc.get("ledgerBalance"))).isEqualByComparingTo("10");
        assertThat(JsonMoney.bd(acc.get("availableBalance"))).isEqualByComparingTo("6");
    }

    private void _onboardLp(String cust) throws Exception {
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"x\"}"
                    .formatted(cust)))
            .andExpect(status().isOk());
    }

    private void _earn(String cust, String amount) throws Exception {
        mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventId":"e-%s","ownerId":"%s","eventType":"PURCHASE",
                     "amount":%s,"currency":"HKD","occurredAt":"%s"}
                    """.formatted(UUID.randomUUID(), cust, amount, Instant.now())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"));
    }

    private void _holdRace(String cust, String amt, CountDownLatch gate, AtomicInteger ok, AtomicInteger fail) {
        try {
            gate.await();
            int status = mockMvc.perform(post("/wallets/holds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"ownerId":"%s","currency":"LP","amount":%s,"movementKey":"race-%s"}
                        """.formatted(cust, amt, UUID.randomUUID())))
                .andReturn().getResponse().getStatus();
            if (status == 200) {
                ok.incrementAndGet();
            } else {
                fail.incrementAndGet();
            }
        } catch (Exception e) {
            fail.incrementAndGet();
        }
    }

    private JsonNode _lp(MvcResult r) throws Exception {
        for (JsonNode a : objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("accounts")) {
            if ("LP".equals(a.get("currency").asText())) {
                return a;
            }
        }
        throw new AssertionError("no LP");
    }
}
