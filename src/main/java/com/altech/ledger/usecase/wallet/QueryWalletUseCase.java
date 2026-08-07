package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Wallet read API (product onboarding surface) including numeric account-set.
 */
@Component
@RequiredArgsConstructor
public class QueryWalletUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
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
        List<Account> set = primary.getMainAccount() == null
            ? List.of(primary)
            : accountRepository.findAllByMainAccount(primary.getMainAccount());
        if (set.isEmpty()) {
            set = List.of(primary);
        }
        List<GetWalletAccountResponseDto> accounts = new ArrayList<>();
        for (Account a : set) {
            boolean isPrimary = primary.getId() != null && primary.getId().equals(a.getId())
                || CoaCodes.isPrimarySub(a.getSubAccount());
            String refCode = isPrimary ? null : _stripLeadingZeros(a.getSubAccount());
            String name = isPrimary && wallet.getNickname() != null
                ? wallet.getNickname()
                : a.getSubAccount();
            accounts.add(DtoWrapper.getWalletAccountResponseDto(a, refCode, isPrimary, name));
        }
        return DtoWrapper.getWalletOnboardResponseDto(wallet, primary, accounts);
    }

    private static String _stripLeadingZeros(String sub) {
        if (sub == null || sub.isBlank()) {
            return null;
        }
        String s = sub.replaceFirst("^0+(?!$)", "");
        return s;
    }
}
