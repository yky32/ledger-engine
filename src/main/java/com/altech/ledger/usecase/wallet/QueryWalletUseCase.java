package com.altech.ledger.usecase.wallet;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.enu.WalletAccountRole;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Wallet read API (product onboarding surface) including account-set.
 */
@Component
@RequiredArgsConstructor
public class QueryWalletUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final IntegrationProperties integrationProperties;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto one(String ownerId, String currency) {
        Wallet wallet = commonUseCase.requireWalletByOwnerAndCurrency(ownerId, currency);
        return _toDto(wallet);
    }

    @Transactional(readOnly = true)
    public List<GetWalletOnboardResponseDto> list(String ownerId) {
        return walletRepository.findByOwnerId(ownerId).stream()
            .map(this::_toDto)
            .toList();
    }

    private GetWalletOnboardResponseDto _toDto(Wallet wallet) {
        Account primary = commonUseCase.requireAccount(wallet.getAccountId());
        String baseRef = integrationProperties.getWalletRefTemplate()
            .replace("{userId}", wallet.getOwnerId() == null ? "" : wallet.getOwnerId())
            .replace("{currency}", wallet.getCurrency().getIsoCode());
        List<Account> set = accountRepository.findAccountSetByWalletRef(baseRef);
        if (set.isEmpty()) {
            set = List.of(primary);
        }
        List<GetWalletAccountResponseDto> accounts = new ArrayList<>();
        for (Account a : set) {
            accounts.add(DtoWrapper.getWalletAccountResponseDto(a, _inferRole(baseRef, a.getFullNumber())));
        }
        return DtoWrapper.getWalletOnboardResponseDto(wallet, primary, accounts);
    }

    private WalletAccountRole _inferRole(String baseRef, String fullNumber) {
        if (fullNumber == null || fullNumber.equals(baseRef)) {
            return WalletAccountRole.MAIN;
        }
        if (!fullNumber.startsWith(baseRef + ":")) {
            return null;
        }
        String code = fullNumber.substring(baseRef.length() + 1);
        for (WalletAccountRole role : WalletAccountRole.values()) {
            if (role.getRefCode().equalsIgnoreCase(code) || role.name().equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }
}
