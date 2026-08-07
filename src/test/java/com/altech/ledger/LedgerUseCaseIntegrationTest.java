package com.altech.ledger;

import com.altech.core.exception.BizException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LedgerUseCaseIntegrationTest {
    @Autowired CreateLedgerAccountUseCase createLedgerAccountUseCase;
    @Autowired QueryLedgerAccountUseCase queryLedgerAccountUseCase;

    @Test
    void createAccountAndReadBalance() {
        AccountResponse cash = createLedgerAccountUseCase.execute(new CreateAccountRequest(
            "cash-usd-1", "Cash USD", CoaType.ASSET, "USD", false));
        assertThat(cash.id()).isNotNull();
        assertThat(cash.ledgerBalance()).isEqualByComparingTo("0");

        BalanceResponse bal = queryLedgerAccountUseCase.balance(cash.id());
        assertThat(bal.accountId()).isEqualTo(cash.id());
        assertThat(bal.currency()).isEqualTo("USD");
        assertThat(bal.ledgerBalance()).isEqualByComparingTo("0");
    }

    @Test
    void duplicateExternalReferenceIsRejected() {
        createLedgerAccountUseCase.execute(new CreateAccountRequest(
            "dup-ref", "One", CoaType.ASSET, "USD", false));
        assertThatThrownBy(() -> createLedgerAccountUseCase.execute(new CreateAccountRequest(
            "dup-ref", "Two", CoaType.ASSET, "USD", false)))
            .isInstanceOf(BizException.class);
    }
}
