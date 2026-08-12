package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Wallet read API for adopted clients.
 * <p>
 * Lookup key = {@code associatedIdentifier} (CUST_ID passed at create).<br>
 * Response = <b>Wallet → accounts[]</b>; optional currencies filter limits account rows.
 */
@Component
@RequiredArgsConstructor
public class QueryWalletUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto byAssociatedIdentifier(String associatedIdentifier) {
        return byAssociatedIdentifier(associatedIdentifier, null);
    }

    /**
     * @param currenciesCsv optional CSV of currency codes, e.g. {@code "HKD,LP"} or {@code "LP"}.
     *                      When set, {@code accounts[]} only includes those currencies.
     *                      Blank / null = all accounts under the wallet.
     */
    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto byAssociatedIdentifier(String associatedIdentifier, String currenciesCsv) {
        Wallet wallet = _requireByAssociatedIdentifier(associatedIdentifier);
        Set<Currency> filter = _parseCurrenciesFilter(currenciesCsv);
        return _toWalletWithAccounts(wallet, filter);
    }

    /** @deprecated use {@link #byAssociatedIdentifier(String)} */
    @Deprecated
    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto one(String ownerId) {
        return byAssociatedIdentifier(ownerId);
    }

    /** @deprecated use {@link #byAssociatedIdentifier(String, String)} */
    @Deprecated
    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto one(String ownerId, String currency) {
        return byAssociatedIdentifier(ownerId, currency);
    }

    /** @deprecated clients should use single-object GET by associatedIdentifier */
    @Deprecated
    @Transactional(readOnly = true)
    public List<GetWalletOnboardResponseDto> list(String ownerId) {
        try {
            return List.of(byAssociatedIdentifier(ownerId));
        } catch (BizException ex) {
            return List.of();
        }
    }

    private Wallet _requireByAssociatedIdentifier(String associatedIdentifier) {
        if (associatedIdentifier == null || associatedIdentifier.isBlank()) {
            throw new BizException(WalletErrorResponse.WAL0400, "associatedIdentifier is required");
        }
        String id = associatedIdentifier.trim();
        return walletRepository.findByOwnerId(id)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found for associatedIdentifier: " + id));
    }

    /**
     * @param currencyFilter empty = no filter (all accounts); otherwise keep only matching ccy
     */
    private GetWalletOnboardResponseDto _toWalletWithAccounts(Wallet wallet, Set<Currency> currencyFilter) {
        Account primary = commonUseCase.requireAccount(wallet.getAccountId());
        List<Account> set = primary.getMainAccount() == null
            ? List.of(primary)
            : accountRepository.findAllByMainAccount(primary.getMainAccount());
        if (set.isEmpty()) {
            set = List.of(primary);
        }

        List<Account> ordered = set.stream()
            .sorted(Comparator
                .comparing((Account a) -> !(primary.getId() != null && primary.getId().equals(a.getId())
                    || CoaCodes.isPrimarySub(a.getSubAccount())))
                .thenComparing(a -> a.getCurrency() == null ? "" : a.getCurrency().getIsoCode())
                .thenComparing(Account::getId))
            .toList();

        if (!currencyFilter.isEmpty()) {
            ordered = ordered.stream()
                .filter(a -> a.getCurrency() != null && currencyFilter.contains(a.getCurrency()))
                .toList();
        }

        List<GetWalletAccountResponseDto> accounts = new ArrayList<>();
        for (Account a : ordered) {
            boolean isPrimary = primary.getId() != null && primary.getId().equals(a.getId())
                || CoaCodes.isPrimarySub(a.getSubAccount());
            String refCode = isPrimary ? null
                : (a.getCurrency() != null ? a.getCurrency().getIsoCode() : _stripLeadingZeros(a.getSubAccount()));
            String name = isPrimary && wallet.getName() != null
                ? wallet.getName()
                : (a.getCurrency() != null ? a.getCurrency().getIsoCode() : a.getSubAccount());
            accounts.add(DtoWrapper.getWalletAccountResponseDto(a, refCode, isPrimary, name));
        }

        // Shortcuts align with filtered set when filter is on
        Account shortcutPrimary = primary;
        if (!currencyFilter.isEmpty()) {
            if (primary.getCurrency() != null && currencyFilter.contains(primary.getCurrency())
                && ordered.stream().anyMatch(a -> a.getId().equals(primary.getId()))) {
                shortcutPrimary = primary;
            } else if (!ordered.isEmpty()) {
                shortcutPrimary = ordered.get(0);
            } else {
                GetWalletOnboardResponseDto emptyBooks = DtoWrapper.getWalletOnboardResponseDto(
                    wallet, primary, List.of());
                emptyBooks.setAccount(null);
                emptyBooks.setBalance(null);
                return emptyBooks;
            }
        }

        return DtoWrapper.getWalletOnboardResponseDto(wallet, shortcutPrimary, accounts);
    }

    /**
     * Parse {@code HKD,LP} / {@code HKD, LP} / single {@code LP}. Invalid codes → BizException.
     */
    private Set<Currency> _parseCurrenciesFilter(String currenciesCsv) {
        if (currenciesCsv == null || currenciesCsv.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<Currency> out = new LinkedHashSet<>();
        Arrays.stream(currenciesCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(code -> out.add(commonUseCase.requireCurrency(code.toUpperCase(Locale.ROOT))));
        return out;
    }

    private static String _stripLeadingZeros(String sub) {
        if (sub == null || sub.isBlank()) {
            return null;
        }
        return sub.replaceFirst("^0+(?!$)", "");
    }
}
