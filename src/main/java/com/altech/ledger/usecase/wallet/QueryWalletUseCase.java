package com.altech.ledger.usecase.wallet;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;

import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Wallet read API (TGT: {@code Query*UseCase}).
 */
@Service
@RequiredArgsConstructor
public class QueryWalletUseCase {
    private final WalletRepository wallets;
    private final AccountRepository accounts;

    @Transactional(readOnly = true)
    public GetWalletOnboardResponseDto get(String ownerId, String currency) {
        String ccy = currency == null ? null : currency.trim().toUpperCase();
        Wallet wallet = wallets.findByOwnerIdAndCurrency(ownerId, ccy)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "Wallet not found for " + ownerId + " / " + ccy));
        return toDto(wallet);
    }

    @Transactional(readOnly = true)
    public List<GetWalletOnboardResponseDto> list(String ownerId) {
        return wallets.findByOwnerId(ownerId).stream()
            .map(this::toDto)
            .toList();
    }

    private GetWalletOnboardResponseDto toDto(Wallet wallet) {
        Account account = accounts.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + wallet.getAccountId()));
        return DtoWrapper.getWalletOnboardResponseDto(wallet, account);
    }
}
