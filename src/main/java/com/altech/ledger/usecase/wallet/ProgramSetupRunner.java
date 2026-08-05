package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import com.altech.ledger.entity.po.LedgerAccount;
import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.repository.LedgerAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProgramSetupRunner implements ApplicationRunner {
    private final IntegrationProperties properties;
    private final LedgerUseCase ledgerUseCase;
    private final LedgerAccountRepository accounts;

    public ProgramSetupRunner(IntegrationProperties properties, LedgerUseCase ledgerUseCase,
                              LedgerAccountRepository accounts) {
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
            ensurePool(properties.getExpensePoolRefTemplate(), "Loyalty expense pool", currency,
                LedgerAccount.Type.EXPENSE);
            ensurePool(properties.getLiabilityPoolRefTemplate(), "Loyalty liability pool", currency,
                LedgerAccount.Type.LIABILITY);
            ensurePool(properties.getDepositClearingRefTemplate(), "Deposit clearing pool", currency,
                LedgerAccount.Type.ASSET);
            ensurePool(properties.getWithdrawalClearingRefTemplate(), "Withdrawal clearing pool", currency,
                LedgerAccount.Type.ASSET);
        }
    }

    private void ensurePool(String template, String name, String currency, LedgerAccount.Type type) {
        String ref = template.replace("{currency}", currency);
        if (accounts.existsByExternalReference(ref)) {
            return;
        }
        ledgerUseCase.createAccount(new CreateAccountRequest(ref, name, type, currency, true));
    }
}
