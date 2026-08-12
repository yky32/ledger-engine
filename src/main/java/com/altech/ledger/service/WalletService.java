package com.altech.ledger.service;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public Wallet get(Long id) {
        return walletRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found: " + id));
    }

    @Transactional(readOnly = true)
    public Wallet getByOwnerId(String ownerId) {
        return walletRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found: " + ownerId));
    }

    @Transactional(readOnly = true)
    public Wallet getFromAccountId(Long accountId) {
        return walletRepository.findByAccountId(accountId)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found for account: " + accountId));
    }

    /**
     * Resolve by numeric id, else by ownerId (CRM / associatedIdentifier).
     */
    @Transactional(readOnly = true)
    public Wallet resolve(String idOrOwnerId) {
        try {
            Long id = Long.valueOf(idOrOwnerId);
            return walletRepository.findById(id)
                .or(() -> walletRepository.findByOwnerId(idOrOwnerId))
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found: " + idOrOwnerId));
        } catch (NumberFormatException ex) {
            return getByOwnerId(idOrOwnerId);
        }
    }
}
