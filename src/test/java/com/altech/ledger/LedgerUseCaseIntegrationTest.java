package com.altech.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.BalanceResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.usecase.ledger.CreateLedgerAccountUseCase;
import com.altech.ledger.usecase.ledger.QueryLedgerAccountUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LedgerUseCaseIntegrationTest {
    @Autowired CreateLedgerAccountUseCase createLedgerAccountUseCase;
    @Autowired QueryLedgerAccountUseCase queryLedgerAccountUseCase;

    @Test
    void createAccountAndReadBalance() {
        AccountResponse cash = createLedgerAccountUseCase.execute(new CreateAccountRequest(
            "ignored-label", "Cash USD", CoaType.ASSET, Currency.USD, false));
        assertThat(cash.id()).isNotNull();
        assertThat(cash.externalReference()).matches("\\d+");
        assertThat(cash.ledgerBalance()).isEqualByComparingTo("0");

        BalanceResponse bal = queryLedgerAccountUseCase.balance(cash.id());
        assertThat(bal.accountId()).isEqualTo(cash.id());
        assertThat(bal.currency()).isEqualTo(Currency.USD);
        assertThat(bal.ledgerBalance()).isEqualByComparingTo("0");
    }

    @Test
    void eachCreateGetsUniqueNumericFullNumber() {
        AccountResponse a = createLedgerAccountUseCase.execute(new CreateAccountRequest(
            "a", "One", CoaType.ASSET, Currency.USD, false));
        AccountResponse b = createLedgerAccountUseCase.execute(new CreateAccountRequest(
            "b", "Two", CoaType.ASSET, Currency.USD, false));
        assertThat(a.externalReference()).matches("\\d+");
        assertThat(b.externalReference()).matches("\\d+");
        assertThat(a.externalReference()).isNotEqualTo(b.externalReference());
    }
}
