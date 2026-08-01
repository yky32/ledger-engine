package com.altech.ledger;

import com.altech.ledger.api.LedgerDtos.*;
import com.altech.ledger.application.LedgerException;
import com.altech.ledger.application.LedgerService;
import com.altech.ledger.domain.JournalEntry;
import com.altech.ledger.domain.LedgerAccount;
import com.altech.ledger.infrastructure.JournalEntryRepository;
import com.altech.ledger.infrastructure.JournalTransactionRepository;
import com.altech.ledger.infrastructure.LedgerAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class LedgerServiceIntegrationTest {
    @Autowired LedgerService service;
    @Autowired JournalEntryRepository entryRepository;
    @Autowired JournalTransactionRepository transactionRepository;
    @Autowired LedgerAccountRepository accountRepository;

    private AccountResponse cash;
    private AccountResponse settlement;
    private AccountResponse equity;
    private AccountResponse sgdCash;

    @BeforeEach
    void setUp() {
        entryRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        cash = account("cash", LedgerAccount.Type.ASSET, "USD", false);
        settlement = account("settlement", LedgerAccount.Type.ASSET, "USD", false);
        equity = account("equity", LedgerAccount.Type.EQUITY, "USD", false);
        sgdCash = account("sgd-cash", LedgerAccount.Type.ASSET, "SGD", false);
        fund(cash.id(), "100.00");
    }

    @Test
    void balancedTransferPostsAndDerivesBalances() {
        var result = transfer("transfer-1", cash.id(), settlement.id(), "25.00");

        assertThat(result.created()).isTrue();
        assertThat(result.transaction().entries()).hasSize(2);
        assertThat(service.getBalance(cash.id()).balance()).isEqualByComparingTo("75.00");
        assertThat(service.getBalance(settlement.id()).balance()).isEqualByComparingTo("25.00");
        assertThat(service.getBalance(cash.id()).debitTotal()).isEqualByComparingTo("100.00");
        assertThat(service.getBalance(cash.id()).creditTotal()).isEqualByComparingTo("25.00");
    }

    @Test
    void unbalancedTransactionIsRejected() {
        PostTransactionRequest request = new PostTransactionRequest("unbalanced", null, null, null, List.of(
            entry(cash.id(), JournalEntry.Side.CREDIT, "10.00", "USD"),
            entry(settlement.id(), JournalEntry.Side.DEBIT, "9.00", "USD")
        ));

        assertThatThrownBy(() -> service.post(request))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("UNBALANCED_TRANSACTION"));
        assertThat(service.getBalance(cash.id()).balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void currencyMismatchIsRejected() {
        PostTransactionRequest request = new PostTransactionRequest("wrong-currency", null, null, null, List.of(
            entry(sgdCash.id(), JournalEntry.Side.DEBIT, "10.00", "USD"),
            entry(equity.id(), JournalEntry.Side.CREDIT, "10.00", "USD")
        ));

        assertThatThrownBy(() -> service.post(request))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("CURRENCY_MISMATCH"));
    }

    @Test
    void insufficientBalanceIsRejected() {
        assertThatThrownBy(() -> transfer("too-much", cash.id(), settlement.id(), "100.01"))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("INSUFFICIENT_BALANCE"));
        assertThat(service.getBalance(settlement.id()).balance()).isZero();
    }

    @Test
    void idempotentRetryReturnsSameTransaction() {
        PostTransactionRequest request = transferRequest("same-key", cash.id(), settlement.id(), "5.00");

        var first = service.post(request);
        var retry = service.post(request);

        assertThat(first.created()).isTrue();
        assertThat(retry.created()).isFalse();
        assertThat(retry.transaction().id()).isEqualTo(first.transaction().id());
        assertThat(service.getBalance(settlement.id()).balance()).isEqualByComparingTo("5.00");
    }

    @Test
    void idempotencyKeyWithDifferentPayloadIsRejected() {
        service.post(transferRequest("conflict-key", cash.id(), settlement.id(), "5.00"));

        assertThatThrownBy(() ->
            service.post(transferRequest("conflict-key", cash.id(), settlement.id(), "6.00")))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void reversalCreatesOppositeImmutableEntriesAndRestoresBalances() {
        var posted = transfer("to-reverse", cash.id(), settlement.id(), "40.00").transaction();
        List<UUID> originalEntryIds = posted.entries().stream().map(EntryResponse::id).toList();

        var reversed = service.reverse(posted.id(), new ReversalRequest("reverse-key", "customer correction"));

        assertThat(reversed.created()).isTrue();
        assertThat(reversed.transaction().reversalOf()).isEqualTo(posted.id());
        assertThat(reversed.transaction().entries())
            .extracting(EntryResponse::side)
            .containsExactly(JournalEntry.Side.DEBIT, JournalEntry.Side.CREDIT);
        assertThat(service.getTransaction(posted.id()).entries())
            .extracting(EntryResponse::id).containsExactlyElementsOf(originalEntryIds);
        assertThat(service.getTransaction(posted.id()).status().name()).isEqualTo("REVERSED");
        assertThat(service.getBalance(cash.id()).balance()).isEqualByComparingTo("100.00");
        assertThat(service.getBalance(settlement.id()).balance()).isZero();
    }

    @Test
    void reversalRetryIsIdempotentButDifferentKeyIsRejected() {
        UUID postedId = transfer("reverse-once", cash.id(), settlement.id(), "10.00").transaction().id();
        ReversalRequest request = new ReversalRequest("reversal-idem", "correction");

        var first = service.reverse(postedId, request);
        var retry = service.reverse(postedId, request);

        assertThat(retry.created()).isFalse();
        assertThat(retry.transaction().id()).isEqualTo(first.transaction().id());
        assertThatThrownBy(() ->
            service.reverse(postedId, new ReversalRequest("another-reversal", "correction")))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("ALREADY_REVERSED"));
    }

    private AccountResponse account(String ref, LedgerAccount.Type type, String currency, boolean allowNegative) {
        return service.createAccount(new CreateAccountRequest(ref, ref, type, currency, allowNegative));
    }

    private void fund(UUID accountId, String amount) {
        service.post(new PostTransactionRequest("fund-" + accountId, "opening", null, null, List.of(
            entry(accountId, JournalEntry.Side.DEBIT, amount, "USD"),
            entry(equity.id(), JournalEntry.Side.CREDIT, amount, "USD")
        )));
    }

    private LedgerService.PostingResult transfer(String key, UUID from, UUID to, String amount) {
        return service.post(transferRequest(key, from, to, amount));
    }

    private PostTransactionRequest transferRequest(String key, UUID from, UUID to, String amount) {
        return new PostTransactionRequest(key, "transfer", null, null, List.of(
            entry(from, JournalEntry.Side.CREDIT, amount, "USD"),
            entry(to, JournalEntry.Side.DEBIT, amount, "USD")
        ));
    }

    private EntryRequest entry(UUID accountId, JournalEntry.Side side, String amount, String currency) {
        return new EntryRequest(accountId, side, new BigDecimal(amount), currency, null);
    }
}
