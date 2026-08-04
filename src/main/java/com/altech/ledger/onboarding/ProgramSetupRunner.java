package com.altech.ledger.onboarding;

import com.altech.ledger.api.LedgerDtos.CreateAccountRequest;
import com.altech.ledger.application.LedgerService;
import com.altech.ledger.domain.LedgerAccount;
import com.altech.ledger.integration.IntegrationProperties;
import com.altech.ledger.infrastructure.LedgerAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProgramSetupRunner implements ApplicationRunner {
    private final IntegrationProperties properties;
    private final LedgerService ledgerService;
    private final LedgerAccountRepository accounts;

    public ProgramSetupRunner(IntegrationProperties properties, LedgerService ledgerService,
                              LedgerAccountRepository accounts) {
        this.properties = properties;
        this.ledgerService = ledgerService;
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
        }
    }

    private void ensurePool(String template, String name, String currency, LedgerAccount.Type type) {
        String ref = template.replace("{currency}", currency);
        if (accounts.existsByExternalReference(ref)) {
            return;
        }
        ledgerService.createAccount(new CreateAccountRequest(ref, name, type, currency, true));
    }
}
