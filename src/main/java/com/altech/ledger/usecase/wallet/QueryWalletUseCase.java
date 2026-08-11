package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.response.GetAccountSetResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.enu.AccountRole;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.AccountSet;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.AccountSetRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
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
import java.util.stream.Collectors;

/**
 * Wallet query by associatedIdentifier — full Wallet → accountSets → accounts (Phase A).
 */
@Component
@RequiredArgsConstructor
public class QueryWalletUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final AccountSetRepository accountSetRepository;

    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto byAssociatedIdentifier(String associatedIdentifier) {
        return byAssociatedIdentifier(associatedIdentifier, null);
    }

    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto byAssociatedIdentifier(String associatedIdentifier, String currenciesCsv) {
        Wallet wallet = _requireByAssociatedIdentifier(associatedIdentifier);
        return _toResponse(wallet, _parseCurrencies(currenciesCsv));
    }

    @Transactional(readOnly = true)
    public List<GetWalletOnboardResponseDto> listByAssociatedIdentifier(String associatedIdentifier) {
        return List.of(byAssociatedIdentifier(associatedIdentifier));
    }

    private Wallet _requireByAssociatedIdentifier(String associatedIdentifier) {
        if (associatedIdentifier == null || associatedIdentifier.isBlank()) {
            throw new BizException(WalletErrorResponse.WAL0400, "associatedIdentifier is required");
        }
        String id = associatedIdentifier.trim();
        return walletRepository.findByOwnerId(id)
            .or(() -> {
                List<Wallet> list = walletRepository.findByAssociatedIdentifier(id);
                if (list.size() == 1) {
                    return java.util.Optional.of(list.get(0));
                }
                if (list.isEmpty()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(list.get(0));
            })
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found for associatedIdentifier: " + id));
    }

    private GetWalletOnboardResponseDto _toResponse(Wallet wallet, Set<Currency> currencyFilter) {
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Primary account missing: " + wallet.getAccountId()));

        List<AccountSet> sets = accountSetRepository.findByWalletIdOrderByIdAsc(wallet.getId());
        List<Account> allAccounts = new ArrayList<>();
        List<GetAccountSetResponseDto> setDtos = new ArrayList<>();

        if (sets.isEmpty()) {
            // legacy wallet: accounts by main only
            allAccounts.addAll(accountRepository.findAllByMainAccount(primary.getMainAccount()));
            allAccounts.sort(Comparator.comparing(Account::getId));
        } else {
            for (AccountSet set : sets) {
                List<Account> setAccounts = accountRepository.findByAccountSetIdOrderByIdAsc(set.getId());
                allAccounts.addAll(setAccounts);
                List<GetWalletAccountResponseDto> accDtos = _mapAccounts(setAccounts, wallet.getAccountId(), currencyFilter);
                setDtos.add(DtoWrapper.getAccountSetResponseDto(set, accDtos));
            }
        }

        List<GetWalletAccountResponseDto> flat = _mapAccounts(allAccounts, wallet.getAccountId(), currencyFilter);
        // primary shortcuts: prefer settlement AVAILABLE
        Account primaryView = allAccounts.stream()
            .filter(a -> a.getAccountRole() == AccountRole.AVAILABLE
                && a.getCurrency() == wallet.getSettlementCurrency())
            .findFirst()
            .orElse(primary);
        if (currencyFilter != null && !currencyFilter.isEmpty()
            && !currencyFilter.contains(primaryView.getCurrency())) {
            primaryView = flat.isEmpty() ? primary : accountRepository.findById(flat.get(0).getId()).orElse(primary);
        }

        GetWalletOnboardResponseDto dto = DtoWrapper.getWalletOnboardResponseDto(wallet, primaryView, flat);
        dto.setAccountSets(setDtos.isEmpty() ? null : setDtos);
        return dto;
    }

    private List<GetWalletAccountResponseDto> _mapAccounts(
        List<Account> accounts,
        Long primaryAccountId,
        Set<Currency> currencyFilter
    ) {
        return accounts.stream()
            .filter(a -> currencyFilter == null || currencyFilter.isEmpty() || currencyFilter.contains(a.getCurrency()))
            .sorted(Comparator
                .comparing((Account a) -> a.getCurrency() == null ? "" : a.getCurrency().getIsoCode())
                .thenComparing(a -> a.getAccountRole() == null ? "" : a.getAccountRole().name()))
            .map(a -> {
                boolean isPrimary = primaryAccountId != null && primaryAccountId.equals(a.getId());
                String ref = a.getAccountRole() != null ? a.getAccountRole().name() : null;
                return DtoWrapper.getWalletAccountResponseDto(a, ref, isPrimary, a.getDisplayName());
            })
            .collect(Collectors.toList());
    }

    private Set<Currency> _parseCurrencies(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        Set<Currency> set = new LinkedHashSet<>();
        Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(s -> {
                try {
                    set.add(Currency.get(s.toUpperCase(Locale.ROOT)));
                } catch (RuntimeException ignored) {
                    // skip unknown
                }
            });
        return set.isEmpty() ? null : set;
    }
}
