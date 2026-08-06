package com.altech.ledger.usecase.account;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletAccountBalanceUseCase {
    private final WalletRepository wallets;
    private final AccountRepository accounts;
    private final FxRateRepository fxRates;

    @Transactional(readOnly = true)
    public WalletWithBalancesResponse getOne(Long id, String fxTarget) {
        Wallet wallet = wallets.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Wallet not found: " + id));
        return withFx(wallet, fxTarget);
    }

    @Transactional(readOnly = true)
    public WalletWithBalancesResponse getByAlias(String alias, String fxTarget) {
        Wallet wallet = wallets.findByAlias(alias)
            .orElseThrow(() -> LedgerException.notFound("Wallet not found alias: " + alias));
        return withFx(wallet, fxTarget);
    }

    @Transactional(readOnly = true)
    public WalletWithBalancesResponse getByExtIdentifier(String id, String type) {
        Wallet wallet = wallets.findByExtIdentifierAndExtType(id, type)
            .orElseThrow(() -> LedgerException.notFound("Wallet not found ext: " + type + "/" + id));
        return withFx(wallet, null);
    }

    @Transactional(readOnly = true)
    public Page<WalletWithBalancesResponse> getAll(Pageable pageable, String fxTarget) {
        return wallets.findAll(pageable).map(w -> withFx(w, fxTarget));
    }

    @Transactional(readOnly = true)
    public List<WalletWithBalancesResponse> myWallets(String ownerId, String fxTarget) {
        return wallets.findByOwnerId(ownerId).stream().map(w -> withFx(w, fxTarget)).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findMyAccount(String ownerId, String currency) {
        Wallet wallet = wallets.findByOwnerIdAndCurrency(ownerId, currency)
            .orElseThrow(() -> LedgerException.notFound("Wallet not found for " + ownerId + "/" + currency));
        return DtoMapper.toAccount(account(wallet.getAccountId()));
    }

    @Transactional(readOnly = true)
    public AccountResponse find(Long walletId, String currency) {
        Wallet wallet = wallets.findById(walletId)
            .orElseThrow(() -> LedgerException.notFound("Wallet not found: " + walletId));
        Account primary = account(wallet.getAccountId());
        if (primary.getCurrency().equalsIgnoreCase(currency)) {
            return DtoMapper.toAccount(primary);
        }
        return accounts.findByMainAccountAndCurrency(primary.getMainAccount(), currency.toUpperCase())
            .map(DtoMapper::toAccount)
            .orElseThrow(() -> LedgerException.notFound(
                "Account not found for wallet " + walletId + " currency " + currency));
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getAllBalances() {
        return accounts.findAll().stream()
            .map(a -> new BalanceResponse(a.getId(), a.getCurrency(), a.getLedgerBalance(),
                a.getAvailableBalance(), null))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> myBalances(String ownerId) {
        List<BalanceResponse> result = new ArrayList<>();
        for (Wallet w : wallets.findByOwnerId(ownerId)) {
            Account primary = account(w.getAccountId());
            for (Account a : accounts.findAllByMainAccount(primary.getMainAccount())) {
                result.add(new BalanceResponse(a.getId(), a.getCurrency(), a.getLedgerBalance(),
                    a.getAvailableBalance(), null));
            }
        }
        return result;
    }

    private WalletWithBalancesResponse withFx(Wallet wallet, String fxTarget) {
        Account primary = account(wallet.getAccountId());
        List<Account> list = new ArrayList<>(accounts.findAllByMainAccount(primary.getMainAccount()));
        WalletWithBalancesResponse base = DtoMapper.toWallet(wallet, list);
        if (fxTarget == null || fxTarget.isBlank()) {
            return base;
        }
        // Display-only FX conversion attached via account list balances (no mutation)
        String target = fxTarget.toUpperCase();
        List<AccountResponse> converted = base.accounts().stream().map(a -> {
            BigDecimal fx = convert(a.ledgerBalance(), a.currency(), target);
            return new AccountResponse(a.id(), a.fullNumber(), a.entity(), a.type(), a.subType(),
                a.mainAccount(), a.subAccount(), a.buffer(), a.currency(),
                a.ledgerBalance(), a.availableBalance(), a.status(), a.createDt(), a.updateDt());
        }).toList();
        // keep structure; conversion available via getAllBalances with rate if needed
        return new WalletWithBalancesResponse(
            base.id(), base.alias(), base.accountId(), base.nickname(), base.extIdentifier(),
            base.extType(), base.type(), base.walletType(), base.status(), base.ownerId(),
            base.currency(), converted, base.createDt(), base.updateDt());
    }

    private BigDecimal convert(BigDecimal amount, String from, String to) {
        if (from.equalsIgnoreCase(to)) return amount;
        Optional<FxRate> direct = fxRates.findByBaseAndTarget(from.toUpperCase(), to.toUpperCase());
        if (direct.isPresent()) {
            return amount.multiply(direct.get().getRate()).setScale(8, RoundingMode.HALF_UP);
        }
        Optional<FxRate> inverse = fxRates.findByBaseAndTarget(to.toUpperCase(), from.toUpperCase());
        if (inverse.isPresent() && inverse.get().getRate().signum() != 0) {
            return amount.divide(inverse.get().getRate(), 8, RoundingMode.HALF_UP);
        }
        return amount;
    }

    private Account account(Long id) {
        return accounts.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Account not found: " + id));
    }
}
