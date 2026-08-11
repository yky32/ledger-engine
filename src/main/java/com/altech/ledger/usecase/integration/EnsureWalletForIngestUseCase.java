package com.altech.ledger.usecase.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.enu.AccountRole;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.AccountSet;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.AccountSetRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import com.altech.ledger.usecase.wallet.DefaultAccountSetInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Resolve wallet for ingest: find existing or auto-create full DEFAULT CoA (Phase A), ensure AVAILABLE point book.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnsureWalletForIngestUseCase {
    private final IntegrationProperties integrationProperties;
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final AccountSetRepository accountSetRepository;
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;
    private final DefaultAccountSetInitializer defaultAccountSetInitializer;

    public record ResolveResult(Wallet wallet, boolean provisioned) {}

    @Transactional
    public ResolveResult resolveOrProvision(String associatedIdentifier, Currency pointCurrency) {
        Optional<Wallet> existing = _find(associatedIdentifier);
        if (existing.isPresent()) {
            Wallet w = existing.get();
            _ensureAvailablePointBook(w, pointCurrency);
            return new ResolveResult(w, false);
        }

        if (!integrationProperties.isAutoCreateWallet()) {
            return null;
        }

        IntegrationProperties.AutoWallet defaults = integrationProperties.getAutoWallet();
        if (defaults == null) {
            defaults = new IntegrationProperties.AutoWallet();
        }
        Currency settlement = Currency.get(
            defaults.getSettlementCurrency() == null ? "HKD" : defaults.getSettlementCurrency());
        String name = (defaults.getNamePrefix() == null ? "Auto " : defaults.getNamePrefix())
            + associatedIdentifier;
        String from = defaults.getAssociatedFrom() == null || defaults.getAssociatedFrom().isBlank()
            ? "CRM" : defaults.getAssociatedFrom();

        boolean provisioned = false;
        try {
            // Phase A template opens full HKD+LP CoA (accounts[] ignored)
            createWalletOnboardingUseCase.execute(new CreateWalletOnboardRequestDto(
                associatedIdentifier,
                settlement,
                name,
                from,
                List.of()
            ));
            provisioned = true;
            log.info("auto-created wallet CoA for associatedIdentifier={} settlement={}",
                associatedIdentifier, settlement);
        } catch (BizException ex) {
            String code = ex.getResponse() != null ? ex.getResponse().getCode() : null;
            if (WalletErrorResponse.WAL0409.getCode().equals(code)
                || AccountErrorResponse.ACC0409.getCode().equals(code)) {
                log.info("auto-create race, wallet already exists for {}", associatedIdentifier);
            } else {
                throw ex;
            }
        }

        Wallet wallet = _find(associatedIdentifier)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found after auto-create: " + associatedIdentifier));
        _ensureAvailablePointBook(wallet, pointCurrency);
        return new ResolveResult(wallet, provisioned);
    }

    private Optional<Wallet> _find(String associatedIdentifier) {
        Optional<Wallet> byOwner = walletRepository.findByOwnerId(associatedIdentifier);
        if (byOwner.isPresent()) {
            return byOwner;
        }
        List<Wallet> byAssoc = walletRepository.findByAssociatedIdentifier(associatedIdentifier);
        if (!byAssoc.isEmpty()) {
            return Optional.of(byAssoc.get(0));
        }
        return Optional.empty();
    }

    private void _ensureAvailablePointBook(Wallet wallet, Currency pointCurrency) {
        if (pointCurrency == null) {
            return;
        }
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Primary account missing for wallet " + wallet.getId()));
        Long setId = accountSetRepository.findByWalletIdAndCode(wallet.getId(), AccountSet.CODE_DEFAULT)
            .map(AccountSet::getId)
            .orElse(null);
        defaultAccountSetInitializer.ensureAvailable(primary.getMainAccount(), pointCurrency, setId);
    }
}
