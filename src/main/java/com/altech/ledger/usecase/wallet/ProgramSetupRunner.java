package com.altech.ledger.usecase.wallet;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProgramSetupRunner implements ApplicationRunner {
    private final IntegrationProperties properties;
    private final LedgerUseCase ledgerUseCase;
    private final AccountRepository accounts;

    public ProgramSetupRunner(IntegrationProperties properties, LedgerUseCase ledgerUseCase,
                              AccountRepository accounts) {
        this.properties = properties;
        this.ledgerUseCase = ledgerUseCase;
        this.accounts = accounts;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isProgramSetupEnabled()) {
            return;
        }
        for (String currency : properties.getProgramCurrencies()) {
            ensurePool(properties.getExpensePoolRefTemplate(), "Loyalty expense pool", currency, CoaType.EXPENSE);
            ensurePool(properties.getLiabilityPoolRefTemplate(), "Loyalty liability pool", currency, CoaType.LIABILITY);
            ensurePool(properties.getDepositClearingRefTemplate(), "Deposit clearing pool", currency, CoaType.ASSET);
            ensurePool(properties.getWithdrawalClearingRefTemplate(), "Withdrawal clearing pool", currency, CoaType.ASSET);
        }
    }

    private void ensurePool(String template, String name, String currency, CoaType type) {
        String ref = template.replace("{currency}", currency);
        if (accounts.existsByFullNumber(ref)) {
            return;
        }
        ledgerUseCase.createAccount(new CreateAccountRequest(ref, name, type, currency, true));
    }
}
