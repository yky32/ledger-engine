package com.altech.ledger.service;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;

import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository wallets;

    @Transactional(readOnly = true)
    public Wallet get(Long id) {
        return wallets.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found: " + id));
    }

    @Transactional(readOnly = true)
    public Wallet getByAlias(String alias) {
        return wallets.findByAlias(alias)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found alias: " + alias));
    }

    @Transactional(readOnly = true)
    public Wallet getFromAccountId(Long accountId) {
        return wallets.findByAccountId(accountId)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found for account: " + accountId));
    }

    @Transactional(readOnly = true)
    public Wallet resolve(String idOrAlias) {
        try {
            Long id = Long.valueOf(idOrAlias);
            return wallets.findById(id).or(() -> wallets.findByAlias(idOrAlias))
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found: " + idOrAlias));
        } catch (NumberFormatException ex) {
            return getByAlias(idOrAlias);
        }
    }
}
