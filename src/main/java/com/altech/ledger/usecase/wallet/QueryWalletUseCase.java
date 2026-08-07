package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Wallet read API (product onboarding surface).
 */
@Component
@RequiredArgsConstructor
public class QueryWalletUseCase {
    private final WalletRepository walletRepository;
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
        Account account = commonUseCase.requireAccount(wallet.getAccountId());
        return DtoWrapper.getWalletOnboardResponseDto(wallet, account);
    }
}
