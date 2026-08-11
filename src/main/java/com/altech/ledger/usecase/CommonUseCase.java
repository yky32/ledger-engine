package com.altech.ledger.usecase;

import com.altech.core.constant.enu.Currency;
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
 * <p>
 * Wallet identity is <b>1 CUST ({@code ownerId}) : 1 Wallet</b>. Currency is default
 * settlement metadata, not part of the wallet key.
 */
@Component
@RequiredArgsConstructor
public class CommonUseCase {

    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final LedgerMovementRepository ledgerMovementRepository;

    public Wallet requireWallet(Long id) {
        return _requireWallet(id);
    }

    /** Primary wallet lookup — 1 customer : 1 wallet. */
    public Wallet requireWalletByOwnerId(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new BizException(WalletErrorResponse.WAL0400, "ownerId is required");
        }
        return walletRepository.findByOwnerId(ownerId.trim())
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found for ownerId: " + ownerId));
    }

    /**
     * Resolve wallet by customer id. {@code currency} is ignored for identity
     * (kept for call-site compatibility — default settlement lives on the wallet row).
     */
    public Wallet requireWalletByOwnerAndCurrency(String ownerId, Currency currency) {
        requireCurrency(currency); // still validate enum if provided
        return requireWalletByOwnerId(ownerId);
    }

    public Wallet requireWalletByOwnerAndCurrency(String ownerId, String currency) {
        return requireWalletByOwnerAndCurrency(ownerId, requireCurrency(currency));
    }

    public Wallet requireActiveWalletByOwnerId(String ownerId) {
        Wallet wallet = requireWalletByOwnerId(ownerId);
        _requireActive(wallet);
        return wallet;
    }

    public Wallet requireActiveWallet(String ownerId, Currency currency) {
        Wallet wallet = requireWalletByOwnerAndCurrency(ownerId, currency);
        _requireActive(wallet);
        return wallet;
    }

    public Wallet requireActiveWallet(String ownerId, String currency) {
        return requireActiveWallet(ownerId, requireCurrency(currency));
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

    /**
     * Parse API/path string into a known {@link Currency} (fiat, LP, or crypto).
     */
    public Currency requireCurrency(String currency) {
        return Currency.get(currency);
    }

    public Currency requireCurrency(Currency currency) {
        if (currency == null || currency == Currency.ALL) {
            throw new BizException(AccountErrorResponse.ACC0400, "Currency is required");
        }
        return currency;
    }

    public void requireActive(Wallet wallet) {
        _requireActive(wallet);
    }

    private Wallet _requireWallet(Long id) {
        return walletRepository.findById(id)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "Wallet not found: " + id));
    }

    private Account _requireAccount(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + id));
    }

    private LedgerMovement _requireMovement(Long id) {
        return ledgerMovementRepository.findById(id)
            .orElseThrow(() -> new BizException(MovementErrorResponse.MOV0404, "Movement not found: " + id));
    }

    private void _requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BizException(WalletErrorResponse.WAL0403, "Wallet is not active: " + wallet.getStatus());
        }
    }
}
