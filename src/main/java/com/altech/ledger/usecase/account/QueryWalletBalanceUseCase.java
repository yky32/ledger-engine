package com.altech.ledger.usecase.account;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.parity.LedgerAccountDtos;
import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QueryWalletBalanceUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final FxRateRepository fxRateRepository;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public LedgerWalletDtos.WithBalancesResponse one(Long id, String fxTarget) {
        return _withFx(commonUseCase.requireWallet(id), fxTarget);
    }

    @Transactional(readOnly = true)
    public LedgerWalletDtos.WithBalancesResponse byAlias(String alias, String fxTarget) {
        Wallet wallet = walletRepository.findByAlias(alias)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found alias: " + alias));
        return _withFx(wallet, fxTarget);
    }

    @Transactional(readOnly = true)
    public LedgerWalletDtos.WithBalancesResponse byExtIdentifier(String id, String type) {
        Wallet wallet = walletRepository.findByExtIdentifierAndExtType(id, type)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found ext: " + type + "/" + id));
        return _withFx(wallet, null);
    }

    @Transactional(readOnly = true)
    public Page<LedgerWalletDtos.WithBalancesResponse> list(Pageable pageable, String fxTarget) {
        return walletRepository.findAll(pageable).map(w -> _withFx(w, fxTarget));
    }

    @Transactional(readOnly = true)
    public List<LedgerWalletDtos.WithBalancesResponse> myWallets(String ownerId, String fxTarget) {
        return walletRepository.findByOwnerId(ownerId).stream().map(w -> _withFx(w, fxTarget)).toList();
    }

    @Transactional(readOnly = true)
    public LedgerAccountDtos.Response findMyAccount(String ownerId, String currency) {
        Wallet wallet = commonUseCase.requireWalletByOwnerAndCurrency(ownerId, currency);
        return DtoMapper.toAccount(commonUseCase.requireAccount(wallet.getAccountId()));
    }

    @Transactional(readOnly = true)
    public LedgerAccountDtos.Response find(Long walletId, String currency) {
        Wallet wallet = commonUseCase.requireWallet(walletId);
        Account primary = commonUseCase.requireAccount(wallet.getAccountId());
        if (primary.getCurrency().equalsIgnoreCase(currency)) {
            return DtoMapper.toAccount(primary);
        }
        return accountRepository.findByMainAccountAndCurrency(primary.getMainAccount(), currency.toUpperCase())
            .map(DtoMapper::toAccount)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Account not found for wallet " + walletId + " currency " + currency));
    }

    @Transactional(readOnly = true)
    public List<LedgerAccountDtos.BalanceResponse> allBalances() {
        return accountRepository.findAll().stream()
            .map(a -> new LedgerAccountDtos.BalanceResponse(a.getId(), a.getCurrency(), a.getLedgerBalance(),
                a.getAvailableBalance(), null))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LedgerAccountDtos.BalanceResponse> myBalances(String ownerId) {
        List<LedgerAccountDtos.BalanceResponse> result = new ArrayList<>();
        for (Wallet w : walletRepository.findByOwnerId(ownerId)) {
            Account primary = commonUseCase.requireAccount(w.getAccountId());
            for (Account a : accountRepository.findAllByMainAccount(primary.getMainAccount())) {
                result.add(new LedgerAccountDtos.BalanceResponse(a.getId(), a.getCurrency(), a.getLedgerBalance(),
                    a.getAvailableBalance(), null));
            }
        }
        return result;
    }

    private LedgerWalletDtos.WithBalancesResponse _withFx(Wallet wallet, String fxTarget) {
        Account primary = commonUseCase.requireAccount(wallet.getAccountId());
        List<Account> list = new ArrayList<>(accountRepository.findAllByMainAccount(primary.getMainAccount()));
        LedgerWalletDtos.WithBalancesResponse base = DtoMapper.toWallet(wallet, list);
        if (fxTarget == null || fxTarget.isBlank()) {
            return base;
        }
        String target = fxTarget.toUpperCase();
        List<LedgerAccountDtos.Response> converted = base.accounts().stream().map(a -> {
            _convert(a.ledgerBalance(), a.currency(), target);
            return new LedgerAccountDtos.Response(a.id(), a.fullNumber(), a.entity(), a.type(), a.subType(),
                a.mainAccount(), a.subAccount(), a.buffer(), a.currency(),
                a.ledgerBalance(), a.availableBalance(), a.status(), a.createDt(), a.updateDt());
        }).toList();
        return new LedgerWalletDtos.WithBalancesResponse(
            base.id(), base.alias(), base.accountId(), base.nickname(), base.extIdentifier(),
            base.extType(), base.type(), base.walletType(), base.status(), base.ownerId(),
            base.currency(), converted, base.createDt(), base.updateDt());
    }

    private BigDecimal _convert(BigDecimal amount, String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        Optional<FxRate> direct = fxRateRepository.findByBaseAndTarget(from.toUpperCase(), to.toUpperCase());
        if (direct.isPresent()) {
            return amount.multiply(direct.get().getRate()).setScale(8, RoundingMode.HALF_UP);
        }
        Optional<FxRate> inverse = fxRateRepository.findByBaseAndTarget(to.toUpperCase(), from.toUpperCase());
        if (inverse.isPresent() && inverse.get().getRate().signum() != 0) {
            return amount.divide(inverse.get().getRate(), 8, RoundingMode.HALF_UP);
        }
        return amount;
    }
}
