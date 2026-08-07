package com.altech.ledger.usecase.wallet;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
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
        String id = wallet.getExtIdentifier() != null ? wallet.getExtIdentifier()
            : (wallet.getOwnerId() == null ? "" : wallet.getOwnerId());
        String baseRef = integrationProperties.getWalletRefTemplate()
            .replace("{extIdentifier}", id)
            .replace("{currency}", wallet.getCurrency().getIsoCode());
        List<Account> set = accountRepository.findAccountSetByWalletRef(baseRef);
        if (set.isEmpty()) {
            set = List.of(primary);
        }
        List<GetWalletAccountResponseDto> accounts = new ArrayList<>();
        for (Account a : set) {
            boolean isPrimary = a.getFullNumber() != null && a.getFullNumber().equals(baseRef)
                || (primary.getId() != null && primary.getId().equals(a.getId()));
            String refCode = _refCode(baseRef, a.getFullNumber());
            accounts.add(DtoWrapper.getWalletAccountResponseDto(a, refCode, isPrimary));
        }
        return DtoWrapper.getWalletOnboardResponseDto(wallet, primary, accounts);
    }

    /** Suffix after base ref, or null for primary. Opaque — no product enum. */
    private String _refCode(String baseRef, String fullNumber) {
        if (fullNumber == null || fullNumber.equals(baseRef)) {
            return null;
        }
        if (!fullNumber.startsWith(baseRef + ":")) {
            return null;
        }
        return fullNumber.substring(baseRef.length() + 1);
    }
}
