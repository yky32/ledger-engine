package com.altech.ledger;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entrypoint.
 * <p>
 * On startup (when {@code ledger.integration.program-setup-enabled=true}), seeds
 * program pool accounts used by loyalty earn/burn (expense / liability / clearing).
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class App implements ApplicationRunner {
    private final IntegrationProperties integrationProperties;
    private final LedgerUseCase ledgerUseCase;
    private final AccountRepository accounts;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    /**
     * Startup seed: program pool COA rows (idempotent by {@code full_number}).
     * Toggle: {@code ledger.integration.program-setup-enabled} / {@code LEDGER_PROGRAM_SETUP_ENABLED}.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!integrationProperties.isProgramSetupEnabled()) {
            log.info("Program pool setup skipped (ledger.integration.program-setup-enabled=false)");
            return;
        }
        for (String currency : integrationProperties.getProgramCurrencies()) {
            ensurePool(integrationProperties.getExpensePoolRefTemplate(),
                "Loyalty expense pool", currency, CoaType.EXPENSE);
            ensurePool(integrationProperties.getLiabilityPoolRefTemplate(),
                "Loyalty liability pool", currency, CoaType.LIABILITY);
            ensurePool(integrationProperties.getDepositClearingRefTemplate(),
                "Deposit clearing pool", currency, CoaType.ASSET);
            ensurePool(integrationProperties.getWithdrawalClearingRefTemplate(),
                "Withdrawal clearing pool", currency, CoaType.ASSET);
        }
        log.info("Program pool setup finished for currencies={}",
            integrationProperties.getProgramCurrencies());
    }

    private void ensurePool(String template, String name, String currency, CoaType type) {
        String ref = template.replace("{currency}", currency);
        if (accounts.existsByFullNumber(ref)) {
            return;
        }
        ledgerUseCase.createAccount(new CreateAccountRequest(ref, name, type, currency, true));
        log.info("Created program pool account fullNumber={} type={} currency={}", ref, type, currency);
    }
}
