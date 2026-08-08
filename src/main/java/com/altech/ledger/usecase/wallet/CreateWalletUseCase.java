package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateLedgerAccountRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerWalletRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.CommonService;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.usecase.account.CreateAccountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateWalletUseCase {
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final CreateAccountUseCase createAccountUseCase;
    private final CommonService commonService;
    private final CommonUseCase commonUseCase;

    @Transactional
    public GetLedgerWalletResponseDto execute(CreateLedgerWalletRequestDto dto) {
        Account account = commonUseCase.requireAccount(dto.accountId());

        String alias = _nextAlias();
        Currency currency = dto.currency() != null
            ? commonUseCase.requireCurrency(dto.currency())
            : account.getCurrency();
        String ownerId = dto.ownerId() != null ? dto.ownerId()
            : (dto.associatedIdentifier() != null ? dto.associatedIdentifier() : alias);
        String nickname = dto.nickname() == null || dto.nickname().isBlank() ? "NA" : dto.nickname();
        WalletAssociationType type = dto.type() == null ? WalletAssociationType.CUSTODIAN : dto.type();

        if (walletRepository.existsByOwnerIdAndCurrency(ownerId, currency)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already exists for owner/currency: " + ownerId + "/" + currency);
        }

        Wallet wallet = new Wallet();
        wallet.setAccountId(account.getId());
        wallet.setAlias(alias);
        wallet.setNickname(nickname);
        wallet.setAssociatedIdentifier(dto.associatedIdentifier());
        wallet.setAssociatedFrom(dto.associatedFrom());
        wallet.setType(type);
        wallet.setWalletType(WalletType.CORPORATE);
        wallet.setStatus(WalletStatus.PENDING);
        wallet.setOwnerId(ownerId);
        wallet.setCurrency(currency);
        wallet = walletRepository.save(wallet);
        List<Account> myAccounts = new ArrayList<>(accountRepository.findAllByMainAccount(account.getMainAccount()));
        return DtoMapper.toWallet(wallet, myAccounts);
    }

    /**
     * Main account (+ optional multi-ccy) then wallet row.
     */
    @Transactional
    public GetLedgerWalletResponseDto executeFull(String ownerId, String mainCurrency,
                                                  List<String> extraCurrencies,
                                                  String associatedIdentifier, String associatedFrom) {
        Currency currency = commonUseCase.requireCurrency(mainCurrency);
        String mainAccountNo = commonService.getNextMainAccount();
        GetLedgerAccountResponseDto main = createAccountUseCase.execute(new CreateLedgerAccountRequestDto(
            "10", "99", "00", "NA", mainAccountNo, "0000", currency, false));
        if (extraCurrencies != null && !extraCurrencies.isEmpty()) {
            createAccountUseCase.executeByAssociatedCurrencies(mainAccountNo, extraCurrencies);
        }
        return execute(new CreateLedgerWalletRequestDto(
            main.id(), associatedIdentifier, associatedFrom, WalletAssociationType.CUSTODIAN,
            ownerId, currency, ownerId == null ? "NA" : ownerId));
    }

    private String _nextAlias() {
        return String.valueOf(System.currentTimeMillis() % 1_000_000_000L)
            + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }
}
