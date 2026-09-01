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
 * Wallet read API — lookup key = {@code ownerId}.
 */
@Component
@RequiredArgsConstructor
public class QueryWalletUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto byOwnerId(String ownerId) {
        return byOwnerId(ownerId, null);
    }

    /**
     * @param currenciesCsv optional CSV e.g. {@code "HKD,LP"} — filters accounts[].
     */
    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto byOwnerId(String ownerId, String currenciesCsv) {
        Wallet wallet = _requireByOwnerId(ownerId);
        Set<Currency> filter = _parseCurrenciesFilter(currenciesCsv);
        return _toWalletWithAccounts(wallet, filter);
    }

    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto one(String ownerId) {
        return byOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto one(String ownerId, String currency) {
        return byOwnerId(ownerId, currency);
    }

    @Transactional(readOnly = true)
    public List<GetWalletOnboardResponseDto> list(String ownerId) {
        try {
            return List.of(byOwnerId(ownerId));
        } catch (BizException ex) {
            return List.of();
        }
    }

    /** Admin query list — active wallets, no nested accounts. */
    @Transactional(readOnly = true)
    public List<GetWalletOnboardResponseDto> listAll() {
        return walletRepository.findAllByIsActiveTrueOrderByCreateDtDesc().stream()
            .map(DtoWrapper::getWalletListRowDto)
            .toList();
    }

    private Wallet _requireByOwnerId(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new BizException(WalletErrorResponse.WAL0400, "ownerId is required");
        }
        String id = ownerId.trim();
        return walletRepository.findByOwnerId(id)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found for ownerId: " + id));
    }

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

        return DtoWrapper.getWalletOnboardResponseDto(wallet, primary, accounts);
    }

    private Set<Currency> _parseCurrenciesFilter(String currenciesCsv) {
        if (currenciesCsv == null || currenciesCsv.isBlank()) {
            return Set.of();
        }
        Set<Currency> out = new LinkedHashSet<>();
        for (String part : currenciesCsv.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            try {
                out.add(Currency.get(p.toUpperCase(Locale.ROOT)));
            } catch (Exception ignored) {
                // skip unknown
            }
        }
        return out;
    }

    private static String _stripLeadingZeros(String sub) {
        if (sub == null) {
            return null;
        }
        String s = sub.replaceFirst("^0+(?!$)", "");
        return s.isEmpty() ? "0" : s;
    }
}
