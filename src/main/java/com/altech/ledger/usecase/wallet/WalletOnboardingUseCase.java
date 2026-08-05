package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.dto.wallet.*;
import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.BalanceResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import com.altech.ledger.entity.po.LedgerAccount;
import com.altech.ledger.entity.po.Wallet;
import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.repository.LedgerAccountRepository;
import com.altech.ledger.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletOnboardingUseCase {
    private final IntegrationProperties properties;
    private final LedgerUseCase ledgerUseCase;
    private final LedgerAccountRepository accounts;
    private final WalletRepository wallets;

    public WalletOnboardingUseCase(IntegrationProperties properties, LedgerUseCase ledgerUseCase,
                                   LedgerAccountRepository accounts, WalletRepository wallets) {
        this.properties = properties;
        this.ledgerUseCase = ledgerUseCase;
        this.accounts = accounts;
        this.wallets = wallets;
    }

    @Transactional
    public WalletOnboardResponse onboard(OnboardWalletRequest request) {
        return createWallet(request);
    }

    /** CRM / legacy bulk import — idempotent per user; each wallet commits independently. */
    public BatchOnboardWalletResponse onboardBatch(BatchOnboardWalletRequest request) {
        int created = 0;
        int alreadyExists = 0;
        java.util.List<WalletOnboardResponse> createdWallets = new java.util.ArrayList<>();
        java.util.List<String> existingUserIds = new java.util.ArrayList<>();

        for (OnboardWalletRequest item : request.wallets()) {
            if (wallets.existsByOwnerIdAndCurrency(item.userId(), item.currency())) {
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

    @Transactional(readOnly = true)
    public WalletOnboardResponse getByOwner(String ownerId, String currency) {
        Wallet wallet = wallets.findByOwnerIdAndCurrency(ownerId, currency)
            .orElseThrow(() -> LedgerException.notFound("Wallet not found for " + ownerId + " / " + currency));
        return toResponse(wallet, ledgerUseCase.getAccount(wallet.getAccountId()),
            ledgerUseCase.getBalance(wallet.getAccountId()));
    }

    @Transactional(readOnly = true)
    public java.util.List<WalletOnboardResponse> listByOwner(String ownerId) {
        return wallets.findByOwnerId(ownerId).stream()
            .map(wallet -> toResponse(wallet, ledgerUseCase.getAccount(wallet.getAccountId()),
                ledgerUseCase.getBalance(wallet.getAccountId())))
            .toList();
    }

    private WalletOnboardResponse createWallet(OnboardWalletRequest request) {
        if (wallets.existsByOwnerIdAndCurrency(request.userId(), request.currency())) {
            throw LedgerException.conflict("WALLET_EXISTS",
                "Wallet already onboarded: " + request.userId() + " / " + request.currency());
        }
        String externalReference = walletRef(request.userId(), request.currency());
        if (accounts.existsByExternalReference(externalReference)) {
            throw LedgerException.conflict("WALLET_EXISTS", "Wallet already onboarded: " + externalReference);
        }
        String name = request.name() == null || request.name().isBlank()
            ? "Wallet " + request.userId()
            : request.name();
        AccountResponse account = ledgerUseCase.createAccount(new CreateAccountRequest(
            externalReference, name, LedgerAccount.Type.LIABILITY, request.currency(), false));
        String alias = request.userId() + "-" + request.currency();
        Wallet wallet = wallets.save(new Wallet(account.id(), alias, request.userId(), request.currency(),
            request.externalId(), request.externalType(), name, Wallet.Status.ACTIVE));
        return toResponse(wallet, account, ledgerUseCase.getBalance(account.id()));
    }

    private WalletOnboardResponse toResponse(Wallet wallet, AccountResponse account, BalanceResponse balance) {
        return new WalletOnboardResponse(wallet.getId(), wallet.getAlias(), wallet.getOwnerId(), wallet.getCurrency(),
            wallet.getStatus(), wallet.getExternalId(), wallet.getExternalType(), account, balance);
    }

    public String walletRef(String userId, String currency) {
        return properties.getWalletRefTemplate()
            .replace("{userId}", userId)
            .replace("{currency}", currency);
    }
}
