package com.altech.ledger.onboarding;

import com.altech.ledger.api.LedgerDtos.AccountResponse;
import com.altech.ledger.api.LedgerDtos.CreateAccountRequest;
import com.altech.ledger.application.LedgerException;
import com.altech.ledger.application.LedgerService;
import com.altech.ledger.domain.LedgerAccount;
import com.altech.ledger.integration.IntegrationProperties;
import com.altech.ledger.infrastructure.LedgerAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletOnboardingService {
    private final IntegrationProperties properties;
    private final LedgerService ledgerService;
    private final LedgerAccountRepository accounts;

    public WalletOnboardingService(IntegrationProperties properties, LedgerService ledgerService,
                                   LedgerAccountRepository accounts) {
        this.properties = properties;
        this.ledgerService = ledgerService;
        this.accounts = accounts;
    }

    @Transactional
    public AccountResponse onboard(OnboardWalletRequest request) {
        return createWallet(request);
    }

    /** CRM / legacy bulk import — idempotent per user; each wallet commits independently. */
    public BatchOnboardWalletResponse onboardBatch(BatchOnboardWalletRequest request) {
        int created = 0;
        int alreadyExists = 0;
        java.util.List<AccountResponse> createdWallets = new java.util.ArrayList<>();
        java.util.List<String> existingUserIds = new java.util.ArrayList<>();

        for (OnboardWalletRequest item : request.wallets()) {
            String externalReference = walletRef(item.userId(), item.currency());
            if (accounts.existsByExternalReference(externalReference)) {
                alreadyExists++;
                existingUserIds.add(item.userId());
                continue;
            }
            createdWallets.add(createWallet(item));
            created++;
        }

        return new BatchOnboardWalletResponse(
            request.wallets().size(), created, alreadyExists, createdWallets, existingUserIds);
    }

    private AccountResponse createWallet(OnboardWalletRequest request) {
        String externalReference = walletRef(request.userId(), request.currency());
        if (accounts.existsByExternalReference(externalReference)) {
            throw LedgerException.conflict("WALLET_EXISTS", "Wallet already onboarded: " + externalReference);
        }
        String name = request.name() == null || request.name().isBlank()
            ? "Wallet " + request.userId()
            : request.name();
        return ledgerService.createAccount(new CreateAccountRequest(
            externalReference, name, LedgerAccount.Type.LIABILITY, request.currency(), false));
    }

    public String walletRef(String userId, String currency) {
        return properties.getWalletRefTemplate()
            .replace("{userId}", userId)
            .replace("{currency}", currency);
    }
}
