package com.altech.ledger.usecase.integration;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.request.AccountOpenSpecDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Resolve wallet for ingest: find existing or auto-create (HKD + LP defaults), ensure point book.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnsureWalletForIngestUseCase {
    private final IntegrationProperties integrationProperties;
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;

    public record ResolveResult(Wallet wallet, boolean provisioned) {}

    /**
     * @param associatedIdentifier CUST_ID
     * @param pointCurrency        rule point currency to ensure as account book
     */
    @Transactional
    public ResolveResult resolveOrProvision(String associatedIdentifier, Currency pointCurrency) {
        Optional<Wallet> existing = _find(associatedIdentifier);
        if (existing.isPresent()) {
            Wallet w = existing.get();
            _ensureCurrencyAccount(w, pointCurrency);
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
        Currency ensure = Currency.get(
            defaults.getEnsureCurrency() == null ? "LP" : defaults.getEnsureCurrency());
        if (pointCurrency != null) {
            ensure = pointCurrency;
        }

        boolean provisioned = false;
        try {
            List<AccountOpenSpecDto> extras = List.of();
            if (ensure != settlement) {
                extras = List.of(new AccountOpenSpecDto(
                    ensure.getIsoCode(),
                    ensure.getIsoCode() + " book",
                    false,
                    false,
                    ensure));
            }
            String name = (defaults.getNamePrefix() == null ? "Auto " : defaults.getNamePrefix())
                + associatedIdentifier;
            String from = defaults.getAssociatedFrom() == null || defaults.getAssociatedFrom().isBlank()
                ? "CRM" : defaults.getAssociatedFrom();

            createWalletOnboardingUseCase.execute(new CreateWalletOnboardRequestDto(
                associatedIdentifier,
                settlement,
                name,
                from,
                extras
            ));
            provisioned = true;
            log.info("auto-created wallet for associatedIdentifier={} settlement={} ensure={}",
                associatedIdentifier, settlement, ensure);
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
        _ensureCurrencyAccount(wallet, pointCurrency != null ? pointCurrency : ensure);
        return new ResolveResult(wallet, provisioned);
    }

    private Optional<Wallet> _find(String associatedIdentifier) {
        Optional<Wallet> byOwner = walletRepository.findByOwnerId(associatedIdentifier);
        if (byOwner.isPresent()) {
            return byOwner;
        }
        List<Wallet> byAssoc = walletRepository.findByAssociatedIdentifier(associatedIdentifier);
        if (byAssoc.size() == 1) {
            return Optional.of(byAssoc.get(0));
        }
        if (!byAssoc.isEmpty()) {
            return Optional.of(byAssoc.get(0));
        }
        return Optional.empty();
    }

    /** Open a balance book under the wallet main COA if missing. */
    private void _ensureCurrencyAccount(Wallet wallet, Currency currency) {
        if (currency == null || wallet.getAccountId() == null) {
            return;
        }
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Primary account missing for wallet " + wallet.getId()));
        if (primary.getCurrency() == currency) {
            return;
        }
        Optional<Account> existing = accountRepository.findByMainAccountAndCurrency(
            primary.getMainAccount(), currency);
        if (existing.isPresent()) {
            return;
        }

        // allocate next free sub 0001, 0002, ...
        String sub = CoaCodes.subAccountCode(null, 1);
        int n = 1;
        while (accountRepository.findByMainAccountAndSubAccount(primary.getMainAccount(), sub).isPresent()) {
            n++;
            sub = CoaCodes.subAccountCode(null, n);
            if (n > 99) {
                throw new BizException(AccountErrorResponse.ACC0400,
                    "No free sub-account under main " + primary.getMainAccount());
            }
        }
        CoaType coaType = CoaType.LIABILITY;
        String fullNumber = CoaCodes.fullNumber(primary.getMainAccount(), sub, coaType, currency);
        accountRepository.save(Account.builder()
            .fullNumber(fullNumber)
            .entity(CoaCodes.ENTITY)
            .type(CoaCodes.typeCode(coaType))
            .subType(CoaCodes.SUB_TYPE)
            .mainAccount(primary.getMainAccount())
            .subAccount(sub)
            .buffer(CoaCodes.BUFFER)
            .currency(currency)
            .allowNegative(false)
            .build());
        log.info("ensured {} account under wallet {} main={}", currency, wallet.getId(), primary.getMainAccount());
    }
}
