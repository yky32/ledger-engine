package com.altech.ledger.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerEntry;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.repository.LedgerEntryRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.util.CoaCodes;
import com.altech.ledger.support.DigestionRuleTestData;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountingRulePostingIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DigestionRuleRepository digestionRuleRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired WalletRepository walletRepository;

    @BeforeEach
    void seed() {
        DigestionRuleTestData.ensureDefaultRules(digestionRuleRepository);
    }

    @Test
    void earnWalksAccountingRules_memberBooksArePerWallet() throws Exception {
        String a = "AR-A-" + UUID.randomUUID().toString().substring(0, 8);
        String b = "AR-B-" + UUID.randomUUID().toString().substring(0, 8);
        _openWallet(a);
        _openWallet(b);

        long movementA = _earn(a, "ar-a-" + UUID.randomUUID());
        long movementB = _earn(b, "ar-b-" + UUID.randomUUID());

        List<LedgerEntry> legsA = ledgerEntryRepository.findByTxnId(movementA);
        List<LedgerEntry> legsB = ledgerEntryRepository.findByTxnId(movementB);
        assertThat(legsA).hasSize(2);
        assertThat(legsB).hasSize(2);

        LedgerEntry creditA = _leg(legsA, MovementDirection.CREDIT);
        LedgerEntry debitA = _leg(legsA, MovementDirection.DEBIT);
        LedgerEntry creditB = _leg(legsB, MovementDirection.CREDIT);
        LedgerEntry debitB = _leg(legsB, MovementDirection.DEBIT);

        assertThat(creditA.getTargetId()).isNotEqualTo(creditB.getTargetId());
        assertThat(debitA.getTargetId()).isEqualTo(debitB.getTargetId());

        Account memberA = accountRepository.findById(Long.valueOf(creditA.getTargetId())).orElseThrow();
        Account memberB = accountRepository.findById(Long.valueOf(creditB.getTargetId())).orElseThrow();
        assertThat(memberA.getWalletId()).isNotEqualTo(memberB.getWalletId());
        assertThat(memberA.getEntity()).isEqualTo("01");
        assertThat(memberA.getType()).isEqualTo("01");
        assertThat(memberA.getSubType()).isEqualTo("01");
        assertThat(memberB.getEntity()).isEqualTo("01");
        assertThat(memberB.getType()).isEqualTo("01");
        assertThat(memberB.getSubType()).isEqualTo("01");
        assertThat(memberA.getCurrency()).isEqualTo(Currency.LP);
        assertThat(memberA.getId()).isNotEqualTo(memberB.getId());

        Account house = accountRepository.findById(Long.valueOf(debitA.getTargetId())).orElseThrow();
        assertThat(house.getEntity()).isEqualTo("01");
        assertThat(house.getType()).isEqualTo("02");
        assertThat(house.getSubType()).isEqualTo("01");
        assertThat(house.getCurrency()).isEqualTo(Currency.LP);
        assertThat(house.getCurrency()).isEqualTo(memberA.getCurrency());
        assertThat(debitA.getCurrency()).isEqualTo(creditA.getCurrency());
        assertThat(debitA.getCurrency()).isEqualTo(Currency.LP);
    }

    @Test
    void sameCustomerTwoMainAccounts_autoCreateThenSplitMemberBooks() throws Exception {
        String owner = "AUTO-" + UUID.randomUUID().toString().substring(0, 8);
        String suffix = String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        String card9089 = "9089" + suffix;
        String card9088 = "9088" + suffix;

        long movement9089 = _earn(owner, "ma-9089-" + UUID.randomUUID(), card9089);
        long movement9088 = _earn(owner, "ma-9088-" + UUID.randomUUID(), card9088);

        Wallet wallet = walletRepository.findByOwnerId(owner).orElseThrow();
        Account primary = accountRepository.findById(wallet.getAccountId()).orElseThrow();
        assertThat(primary.getMainAccount()).isEqualTo(card9089);

        LedgerEntry credit9089 = _leg(ledgerEntryRepository.findByTxnId(movement9089), MovementDirection.CREDIT);
        LedgerEntry debit9089 = _leg(ledgerEntryRepository.findByTxnId(movement9089), MovementDirection.DEBIT);
        LedgerEntry credit9088 = _leg(ledgerEntryRepository.findByTxnId(movement9088), MovementDirection.CREDIT);
        LedgerEntry debit9088 = _leg(ledgerEntryRepository.findByTxnId(movement9088), MovementDirection.DEBIT);

        Account member9089 = accountRepository.findById(Long.valueOf(credit9089.getTargetId())).orElseThrow();
        Account member9088 = accountRepository.findById(Long.valueOf(credit9088.getTargetId())).orElseThrow();
        Account house9089 = accountRepository.findById(Long.valueOf(debit9089.getTargetId())).orElseThrow();
        Account house9088 = accountRepository.findById(Long.valueOf(debit9088.getTargetId())).orElseThrow();

        assertThat(member9089.getId()).isNotEqualTo(member9088.getId());
        assertThat(member9089.getWalletId()).isEqualTo(wallet.getId());
        assertThat(member9088.getWalletId()).isEqualTo(wallet.getId());
        assertThat(member9089.getMainAccount()).isEqualTo(card9089);
        assertThat(member9088.getMainAccount()).isEqualTo(card9088);
        assertThat(member9089.getEntity()).isEqualTo("01");
        assertThat(member9089.getType()).isEqualTo("01");
        assertThat(member9089.getSubType()).isEqualTo("01");
        assertThat(member9088.getEntity()).isEqualTo("01");
        assertThat(member9088.getType()).isEqualTo("01");
        assertThat(member9088.getSubType()).isEqualTo("01");

        assertThat(house9089.getId()).isEqualTo(house9088.getId());
        assertThat(house9089.getMainAccount()).isEqualTo(CoaCodes.HOUSE_MAIN_ACCOUNT);
        assertThat(house9089.getWalletId()).isNotEqualTo(wallet.getId());

        List<Account> customerBooks = accountRepository.findAllByWalletId(wallet.getId());
        assertThat(customerBooks).isNotEmpty();
        assertThat(customerBooks).allMatch(a ->
            "01".equals(a.getEntity()) && "01".equals(a.getType()) && "01".equals(a.getSubType()));
        assertThat(customerBooks).noneMatch(a -> "10".equals(a.getEntity()) || "20".equals(a.getType()));
        assertThat(customerBooks).anyMatch(a -> card9089.equals(a.getMainAccount()) && a.getCurrency() == Currency.HKD);
        assertThat(customerBooks).anyMatch(a -> card9088.equals(a.getMainAccount()) && a.getCurrency() == Currency.HKD);
    }

    @Test
    void ingestOpensOnlyCustomerCustodianPerMainAccount() throws Exception {
        String owner = "01A" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        String suffix = String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100_000_000));
        String card9089 = "9089" + suffix;
        String card9088 = "9088" + suffix;

        _seedDigestion("CC_TXN_LP_" + UUID.randomUUID().toString().substring(0, 6), "CC_TXN", "LP");
        _earnEventWithMain(owner, "CC_TXN", new BigDecimal("100"), card9089);

        _seedDigestion("CC_TXN_HKD_" + UUID.randomUUID().toString().substring(0, 6), "CC_TXN", "HKD");
        _earnEventWithMain(owner, "CC_TXN", new BigDecimal("100"), card9088);

        Wallet wallet = walletRepository.findByOwnerId(owner).orElseThrow();
        assertThat(wallet.getSettlementCurrency()).isEqualTo(Currency.HKD);
        List<Account> books = accountRepository.findAllByWalletId(wallet.getId());
        assertThat(books).allMatch(a ->
            "01".equals(a.getEntity()) && "01".equals(a.getType()) && "01".equals(a.getSubType()));
        assertThat(books).anyMatch(a -> card9089.equals(a.getMainAccount()) && a.getCurrency() == Currency.HKD);
        assertThat(books).anyMatch(a -> card9089.equals(a.getMainAccount()) && a.getCurrency() == Currency.LP);
        assertThat(books).anyMatch(a -> card9088.equals(a.getMainAccount()) && a.getCurrency() == Currency.HKD);
        assertThat(books.stream().map(Account::getMainAccount).distinct())
            .allMatch(main -> books.stream().anyMatch(a ->
                main.equals(a.getMainAccount()) && a.getCurrency() == wallet.getSettlementCurrency()));
    }

    @Test
    void ccTxnLoyalty_resultCurrencyLp_booksLp() throws Exception {
        _seedDigestion("CC_TXN_LP_" + UUID.randomUUID().toString().substring(0, 6),
            "CC_TXN", "LP");
        long movementId = _earnEvent("CT-" + UUID.randomUUID().toString().substring(0, 8),
            "CC_TXN", new BigDecimal("100"));
        _assertSameCurrencyBooks(movementId, Currency.LP);
    }

    @Test
    void ccTxnCashback_resultCurrencyHkd_booksHkd() throws Exception {
        _seedDigestion("CC_TXN_HKD_" + UUID.randomUUID().toString().substring(0, 6),
            "CC_TXN", "HKD");
        long movementId = _earnEvent("CH-" + UUID.randomUUID().toString().substring(0, 8),
            "CC_TXN", new BigDecimal("100"));
        _assertSameCurrencyBooks(movementId, Currency.HKD);
    }

    @Test
    void ccCipAndLnTxn_followResultCurrency() throws Exception {
        _seedDigestion("CC_CIP_" + UUID.randomUUID().toString().substring(0, 6),
            "CC_CIP", "LP");
        _seedDigestion("LN_TXN_" + UUID.randomUUID().toString().substring(0, 6),
            "LN_TXN", "HKD");
        _assertSameCurrencyBooks(
            _earnEvent("CIP-" + UUID.randomUUID().toString().substring(0, 8),
                "CC_CIP", new BigDecimal("100")),
            Currency.LP);
        _assertSameCurrencyBooks(
            _earnEvent("LN-" + UUID.randomUUID().toString().substring(0, 8),
                "LN_TXN", new BigDecimal("100")),
            Currency.HKD);
    }

    @Test
    void transactionToHkd_eventTypeBooksHkdEvenIfBrainSaysLp() throws Exception {
        _seedDigestion("TXN_HKD_" + UUID.randomUUID().toString().substring(0, 6),
            "CC_TXN_HKD", "LP");
        long movementId = _earnEvent("TH-" + UUID.randomUUID().toString().substring(0, 8),
            "CC_TXN_HKD", new BigDecimal("100"));
        _assertSameCurrencyBooks(movementId, Currency.HKD);
    }

    @Test
    void transactionToLp_eventTypeBooksLp() throws Exception {
        _seedDigestion("TXN_LP_" + UUID.randomUUID().toString().substring(0, 6),
            "CC_TXN_LP", "LP");
        long movementId = _earnEvent("TL-" + UUID.randomUUID().toString().substring(0, 8),
            "CC_TXN_LP", new BigDecimal("100"));
        _assertSameCurrencyBooks(movementId, Currency.LP);
    }

    @Test
    void unboundEventWithHkdReward_fallsBackToCashbackBooks() throws Exception {
        String eventType = "UNBOUND_CB";
        _seedDigestion("UNB_HKD_" + UUID.randomUUID().toString().substring(0, 6),
            eventType, "HKD");
        long movementId = _earnEvent("UB-" + UUID.randomUUID().toString().substring(0, 8),
            eventType, new BigDecimal("100"));
        _assertSameCurrencyBooks(movementId, Currency.HKD);
    }

    @Test
    void rewardHkdCashback_booksSameCurrencyHkd() throws Exception {
        _seedDigestion("CASHBACK_HKD_" + UUID.randomUUID().toString().substring(0, 8),
            "CC_TXN", "HKD");
        long movementId = _earnEvent("CB-" + UUID.randomUUID().toString().substring(0, 8),
            "CC_TXN", new BigDecimal("5"));
        _assertSameCurrencyBooks(movementId, Currency.HKD);
    }

    private void _seedDigestion(String code, String eventType, String resultCurrency) throws Exception {
        digestionRuleRepository.findAll().stream()
            .filter(r -> eventType.equalsIgnoreCase(r.getEventType()) && Boolean.TRUE.equals(r.getIsEnabled()))
            .forEach(r -> {
                r.setIsEnabled(false);
                digestionRuleRepository.save(r);
            });
        mockMvc.perform(post("/digestion-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"%s","name":"%s","eventType":"%s","operation":"EARN",
                     "resultCurrency":"%s","priority":5,"isEnabled":true,
                     "formula":{"type":"RATE","rate":0.01}}
                    """.formatted(code, code, eventType, resultCurrency)))
            .andExpect(status().isOk());
    }

    private long _earnEvent(String ownerId, String eventType, BigDecimal amount) throws Exception {
        return _earnEventWithMain(ownerId, eventType, amount, null);
    }

    private long _earnEventWithMain(String ownerId, String eventType, BigDecimal amount, String mainAccount)
        throws Exception {
        TransactionalEvent event = new TransactionalEvent(
            "evt-" + UUID.randomUUID(), ownerId, eventType, amount, Currency.HKD, Instant.now(), Map.of(),
            mainAccount);
        MvcResult res = mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EARNED"))
            .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
            .get("data").get("movementId").asLong();
    }

    private void _assertSameCurrencyBooks(long movementId, Currency expected) {
        LedgerEntry credit = _leg(ledgerEntryRepository.findByTxnId(movementId), MovementDirection.CREDIT);
        LedgerEntry debit = _leg(ledgerEntryRepository.findByTxnId(movementId), MovementDirection.DEBIT);
        Account member = accountRepository.findById(Long.valueOf(credit.getTargetId())).orElseThrow();
        Account house = accountRepository.findById(Long.valueOf(debit.getTargetId())).orElseThrow();
        assertThat(member.getEntity()).isEqualTo("01");
        assertThat(member.getType()).isEqualTo("01");
        assertThat(member.getSubType()).isEqualTo("01");
        assertThat(house.getEntity()).isEqualTo("01");
        assertThat(house.getType()).isEqualTo("02");
        assertThat(house.getSubType()).isEqualTo("01");
        assertThat(member.getCurrency()).isEqualTo(expected);
        assertThat(house.getCurrency()).isEqualTo(expected);
        assertThat(credit.getCurrency()).isEqualTo(expected);
        assertThat(debit.getCurrency()).isEqualTo(expected);
    }

    private void _openWallet(String ownerId) throws Exception {
        mockMvc.perform(post("/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"%s\",\"settlementCurrency\":\"LP\",\"name\":\"ar\"}"
                    .formatted(ownerId)))
            .andExpect(status().isOk());
    }

    private long _earn(String ownerId, String eventId) throws Exception {
        return _earn(ownerId, eventId, null);
    }

    private long _earn(String ownerId, String eventId, String mainAccount) throws Exception {
        TransactionalEvent event = new TransactionalEvent(
            eventId, ownerId, "PURCHASE", new BigDecimal("100"), Currency.HKD, Instant.now(), Map.of(),
            mainAccount);
        MvcResult res = mockMvc.perform(post("/integrations/webhooks/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andReturn();
        String body = res.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(res.getResponse().getStatus())
            .as("webhook body=%s", body)
            .isEqualTo(200);
        JsonNode root = objectMapper.readTree(body);
        org.assertj.core.api.Assertions.assertThat(root.path("data").path("status").asText())
            .as("webhook body=%s", body)
            .isEqualTo("EARNED");
        org.assertj.core.api.Assertions.assertThat(root.path("data").path("legs").size())
            .as("webhook body=%s", body)
            .isEqualTo(2);
        return root.get("data").get("movementId").asLong();
    }

    private static LedgerEntry _leg(List<LedgerEntry> legs, MovementDirection direction) {
        return legs.stream().filter(e -> e.getDirection() == direction).findFirst().orElseThrow();
    }
}
