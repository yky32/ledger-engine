package com.altech.ledger;

import com.altech.ledger.entity.dto.ledger.LedgerDto.*;
import com.altech.ledger.entity.po.journal.JournalEntry;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.JournalEntryRepository;
import com.altech.ledger.repository.JournalTransactionRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class LedgerUseCaseIntegrationTest {
    @Autowired LedgerUseCase useCase;
    @Autowired JournalEntryRepository entryRepository;
    @Autowired JournalTransactionRepository transactionRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired LedgerMovementRepository movementRepository;
    @Autowired WalletRepository walletRepository;

    private AccountResponse cash;
    private AccountResponse settlement;
    private AccountResponse equity;
    private AccountResponse sgdCash;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        walletRepository.deleteAll();
        entryRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        cash = account("cash", CoaType.ASSET, "USD", false);
        settlement = account("settlement", CoaType.ASSET, "USD", false);
        equity = account("equity", CoaType.EQUITY, "USD", false);
        sgdCash = account("sgd-cash", CoaType.ASSET, "SGD", false);
        fund(cash.id(), "100.00");
    }

    @Test
    void balancedTransferPostsAndDerivesBalances() {
        var result = transfer("transfer-1", cash.id(), settlement.id(), "25.00");

        assertThat(result.created()).isTrue();
        assertThat(result.transaction().entries()).hasSize(2);
        assertThat(useCase.getBalance(cash.id()).balance()).isEqualByComparingTo("75.00");
        assertThat(useCase.getBalance(settlement.id()).balance()).isEqualByComparingTo("25.00");
        assertThat(useCase.getBalance(cash.id()).debitTotal()).isEqualByComparingTo("100.00");
        assertThat(useCase.getBalance(cash.id()).creditTotal()).isEqualByComparingTo("25.00");
        assertThat(useCase.getBalance(cash.id()).ledgerBalance()).isEqualByComparingTo("75.00");
    }

    @Test
    void unbalancedTransactionIsRejected() {
        PostTransactionRequest request = new PostTransactionRequest("unbalanced", null, null, null, List.of(
            entry(cash.id(), JournalEntry.Side.CREDIT, "10.00", "USD"),
            entry(settlement.id(), JournalEntry.Side.DEBIT, "9.00", "USD")
        ));

        assertThatThrownBy(() -> useCase.post(request))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("UNBALANCED_TRANSACTION"));
        assertThat(useCase.getBalance(cash.id()).balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void currencyMismatchIsRejected() {
        PostTransactionRequest request = new PostTransactionRequest("wrong-currency", null, null, null, List.of(
            entry(sgdCash.id(), JournalEntry.Side.DEBIT, "10.00", "USD"),
            entry(equity.id(), JournalEntry.Side.CREDIT, "10.00", "USD")
        ));

        assertThatThrownBy(() -> useCase.post(request))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("CURRENCY_MISMATCH"));
    }

    @Test
    void insufficientBalanceIsRejected() {
        assertThatThrownBy(() -> transfer("too-much", cash.id(), settlement.id(), "100.01"))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("INSUFFICIENT_BALANCE"));
        assertThat(useCase.getBalance(settlement.id()).balance()).isZero();
    }

    @Test
    void idempotentRetryReturnsSameTransaction() {
        PostTransactionRequest request = transferRequest("same-key", cash.id(), settlement.id(), "5.00");
        var first = useCase.post(request);
        var second = useCase.post(request);
        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.transaction().id()).isEqualTo(first.transaction().id());
    }

    @Test
    void idempotencyConflictWhenPayloadDiffers() {
        useCase.post(transferRequest("conflict-key", cash.id(), settlement.id(), "5.00"));
        assertThatThrownBy(() -> useCase.post(transferRequest("conflict-key", cash.id(), settlement.id(), "6.00")))
            .isInstanceOfSatisfying(LedgerException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    private void fund(Long cashId, String amount) {
        useCase.post(new PostTransactionRequest("fund-" + cashId, "opening", null, null, List.of(
            entry(cashId, JournalEntry.Side.DEBIT, amount, "USD"),
            entry(equity.id(), JournalEntry.Side.CREDIT, amount, "USD")
        )));
    }

    private LedgerUseCase.PostingResult transfer(String key, Long from, Long to, String amount) {
        return useCase.post(transferRequest(key, from, to, amount));
    }

    private PostTransactionRequest transferRequest(String key, Long from, Long to, String amount) {
        return new PostTransactionRequest(key, null, null, null, List.of(
            entry(from, JournalEntry.Side.CREDIT, amount, "USD"),
            entry(to, JournalEntry.Side.DEBIT, amount, "USD")
        ));
    }

    private EntryRequest entry(Long accountId, JournalEntry.Side side, String amount, String currency) {
        return new EntryRequest(accountId, side, new BigDecimal(amount), currency, null);
    }

    private AccountResponse account(String ref, CoaType type, String currency, boolean allowNegative) {
        return useCase.createAccount(new CreateAccountRequest(ref, ref, type, currency, allowNegative));
    }
}
