package com.altech.ledger.usecase.wallet;

import com.altech.core.exception.BizException;
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
import com.altech.ledger.entity.dto.request.CreateLedgerAccountRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerWalletRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;

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
        String currency = dto.currency() != null
            ? commonUseCase.normalizeCurrency(dto.currency())
            : account.getCurrency();
        String ownerId = dto.ownerId() != null ? dto.ownerId()
            : (dto.extIdentifier() != null ? dto.extIdentifier() : alias);
        String nickname = dto.nickname() == null || dto.nickname().isBlank() ? "NA" : dto.nickname();
        WalletAssociationType type = dto.type() == null ? WalletAssociationType.CUSTODIAN : dto.type();

        if (walletRepository.existsByOwnerIdAndCurrency(ownerId, currency)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already exists for owner/currency: " + ownerId + "/" + currency);
        }

        Wallet wallet = new Wallet(
            account.getId(), alias, nickname, dto.extIdentifier(), dto.extType(),
            type, WalletType.CORPORATE, WalletStatus.PENDING, ownerId, currency);
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
                                                             String extIdentifier, String extType) {
        String mainAccountNo = commonService.getNextMainAccount();
        GetLedgerAccountResponseDto main = createAccountUseCase.execute(new CreateLedgerAccountRequestDto(
            "10", "99", "00", "NA", mainAccountNo, "0000", mainCurrency.toUpperCase(), false));
        if (extraCurrencies != null && !extraCurrencies.isEmpty()) {
            createAccountUseCase.executeByAssociatedCurrencies(mainAccountNo, extraCurrencies);
        }
        return execute(new CreateLedgerWalletRequestDto(
            main.id(), extIdentifier, extType, WalletAssociationType.CUSTODIAN,
            ownerId, mainCurrency.toUpperCase(), ownerId == null ? "NA" : ownerId));
    }

    private String _nextAlias() {
        return String.valueOf(System.currentTimeMillis() % 1_000_000_000L)
            + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }
}
