package com.altech.ledger.usecase;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting entity loaders and validators shared by {@code *UseCase} classes.
 * Private helpers use the {@code _name} prefix; public methods are the stable API.
 */
@Component
@RequiredArgsConstructor
public class CommonUseCase {

    private final WalletRepository wallets;
    private final AccountRepository accounts;
    private final LedgerMovementRepository movements;

    public Wallet requireWallet(Long id) {
        return _requireWallet(id);
    }

    public Wallet requireWalletByOwnerAndCurrency(String ownerId, String currency) {
        String ccy = normalizeCurrency(currency);
        return wallets.findByOwnerIdAndCurrency(ownerId, ccy)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found for " + ownerId + " / " + ccy));
    }

    public Wallet requireActiveWallet(String ownerId, String currency) {
        Wallet wallet = requireWalletByOwnerAndCurrency(ownerId, currency);
        _requireActive(wallet);
        return wallet;
    }

    public Wallet requireActiveWallet(Long id) {
        Wallet wallet = _requireWallet(id);
        _requireActive(wallet);
        return wallet;
    }

    public Account requireAccount(Long id) {
        return _requireAccount(id);
    }

    public LedgerMovement requireMovement(Long id) {
        return _requireMovement(id);
    }

    public String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase();
    }

    public String requireCurrency(String currency) {
        String ccy = normalizeCurrency(currency);
        if (ccy == null || !ccy.matches("[A-Z]{2,4}")) {
            throw new BizException(AccountErrorResponse.ACC0400, "Currency must be 2-4 uppercase letters");
        }
        return ccy;
    }

    public void requireActive(Wallet wallet) {
        _requireActive(wallet);
    }

    private Wallet _requireWallet(Long id) {
        return wallets.findById(id)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "Wallet not found: " + id));
    }

    private Account _requireAccount(Long id) {
        return accounts.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + id));
    }

    private LedgerMovement _requireMovement(Long id) {
        return movements.findById(id)
            .orElseThrow(() -> new BizException(MovementErrorResponse.MOV0404, "Movement not found: " + id));
    }

    private void _requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BizException(WalletErrorResponse.WAL0403, "Wallet is not active: " + wallet.getStatus());
        }
    }
}
