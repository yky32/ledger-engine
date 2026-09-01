package com.altech.ledger.usecase.account;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
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
import com.altech.ledger.util.Pageables;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.altech.ledger.entity.dto.response.GetLedgerAccountBalanceResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;

@Component
@RequiredArgsConstructor
public class QueryWalletBalanceUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final FxRateRepository fxRateRepository;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public GetLedgerWalletResponseDto one(Long id, String fxTarget) {
        return _withFx(commonUseCase.requireWallet(id), fxTarget);
    }

    @Transactional(readOnly = true)
    public GetLedgerWalletResponseDto byAlias(String alias, String fxTarget) {
        // alias retired — treat as ownerId
        Wallet wallet = walletRepository.findByOwnerId(alias)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found: " + alias));
        return _withFx(wallet, fxTarget);
    }

    @Transactional(readOnly = true)
    public GetLedgerWalletResponseDto byAssociatedIdentifier(String id, String typeIgnored) {
        Wallet wallet = walletRepository.findByOwnerId(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Wallet not found: " + id));
        return _withFx(wallet, null);
    }

    @Transactional(readOnly = true)
    public Page<GetLedgerWalletResponseDto> list(Pageable pageable, String fxTarget) {
        return walletRepository.findAll(Pageables.toZeroBased(pageable)).map(w -> _withFx(w, fxTarget));
    }

    @Transactional(readOnly = true)
    public List<GetLedgerWalletResponseDto> myWallets(String ownerId, String fxTarget) {
        return walletRepository.findAllByOwnerId(ownerId).stream().map(w -> _withFx(w, fxTarget)).toList();
    }

    @Transactional(readOnly = true)
    public GetLedgerAccountResponseDto findMyAccount(String ownerId, String currency) {
        Wallet wallet = commonUseCase.requireWalletByOwnerAndCurrency(ownerId, currency);
        return DtoMapper.toAccount(commonUseCase.requireAccount(wallet.getAccountId()));
    }

    @Transactional(readOnly = true)
    public GetLedgerAccountResponseDto find(Long walletId, String currencyCode) {
        Currency currency = commonUseCase.requireCurrency(currencyCode);
        Wallet wallet = commonUseCase.requireWallet(walletId);
        Account primary = commonUseCase.requireAccount(wallet.getAccountId());
        if (primary.getCurrency() == currency) {
            return DtoMapper.toAccount(primary);
        }
        return accountRepository.findFirstByMainAccountAndCurrency(primary.getMainAccount(), currency)
            .map(DtoMapper::toAccount)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Account not found for wallet " + walletId + " currency " + currency));
    }

    @Transactional(readOnly = true)
    public List<GetLedgerAccountBalanceResponseDto> allBalances() {
        return accountRepository.findAll().stream()
            .map(a -> new GetLedgerAccountBalanceResponseDto(a.getId(), a.getCurrency(), a.getLedgerBalance(),
                a.getAvailableBalance(), null))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<GetLedgerAccountBalanceResponseDto> myBalances(String ownerId) {
        List<GetLedgerAccountBalanceResponseDto> result = new ArrayList<>();
        for (Wallet w : walletRepository.findAllByOwnerId(ownerId)) {
            Account primary = commonUseCase.requireAccount(w.getAccountId());
            List<Account> books = accountRepository.findAllByWalletId(w.getId());
            if (books.isEmpty()) {
                books = accountRepository.findAllByMainAccount(primary.getMainAccount());
            }
            for (Account a : books) {
                result.add(new GetLedgerAccountBalanceResponseDto(a.getId(), a.getCurrency(), a.getLedgerBalance(),
                    a.getAvailableBalance(), null));
            }
        }
        return result;
    }

    private GetLedgerWalletResponseDto _withFx(Wallet wallet, String fxTarget) {
        Account primary = commonUseCase.requireAccount(wallet.getAccountId());
        List<Account> list = new ArrayList<>(accountRepository.findAllByWalletId(wallet.getId()));
        if (list.isEmpty() && primary.getMainAccount() != null) {
            list.addAll(accountRepository.findAllByMainAccount(primary.getMainAccount()));
        }
        GetLedgerWalletResponseDto base = DtoMapper.toWallet(wallet, list);
        if (fxTarget == null || fxTarget.isBlank()) {
            return base;
        }
        Currency target = commonUseCase.requireCurrency(fxTarget);
        List<GetLedgerAccountResponseDto> converted = base.accounts().stream().map(a -> {
            _convert(a.ledgerBalance(), a.currency(), target);
            return new GetLedgerAccountResponseDto(a.id(), a.fullNumber(), a.entity(), a.type(), a.subType(),
                a.mainAccount(), a.subAccount(), a.buffer(), a.currency(),
                a.ledgerBalance(), a.availableBalance(), a.status(), a.createDt(), a.updateDt());
        }).toList();
        return new GetLedgerWalletResponseDto(
            base.id(), base.accountId(), base.ownerId(), base.vanityCode(), base.name(),
            base.type(), base.walletType(), base.status(), base.settlementCurrency(),
            converted, base.createDt(), base.updateDt());
    }

    private BigDecimal _convert(BigDecimal amount, Currency from, Currency to) {
        if (from == to) {
            return amount;
        }
        Optional<FxRate> direct = fxRateRepository.findByBaseAndTarget(from, to);
        if (direct.isPresent()) {
            return amount.multiply(direct.get().getRate()).setScale(8, RoundingMode.HALF_UP);
        }
        Optional<FxRate> inverse = fxRateRepository.findByBaseAndTarget(to, from);
        if (inverse.isPresent() && inverse.get().getRate().signum() != 0) {
            return amount.divide(inverse.get().getRate(), 8, RoundingMode.HALF_UP);
        }
        return amount;
    }
}

