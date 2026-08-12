package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateLedgerAccountRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerWalletRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;
import com.altech.ledger.entity.enu.WalletStatus;
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

        Currency settlementCurrency = dto.settlementCurrency() != null
            ? commonUseCase.requireCurrency(dto.settlementCurrency())
            : account.getCurrency();
        String ownerId = dto.ownerId() != null && !dto.ownerId().isBlank()
            ? dto.ownerId().trim()
            : (dto.associatedIdentifier() != null ? dto.associatedIdentifier().trim() : null);
        if (ownerId == null || ownerId.isBlank()) {
            throw new BizException(WalletErrorResponse.WAL0400, "ownerId / associatedIdentifier required");
        }
        if (walletRepository.existsByOwnerId(ownerId)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already exists for owner: " + ownerId);
        }

        Wallet wallet = new Wallet();
        wallet.setAccountId(account.getId());
        wallet.setOwnerId(ownerId);
        wallet.setName(dto.name() == null || dto.name().isBlank() ? null : dto.name().trim());
        wallet.setStatus(WalletStatus.PENDING);
        wallet.setSettlementCurrency(settlementCurrency);
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
                                                  String associatedIdentifier, String associatedFromIgnored) {
        Currency currency = commonUseCase.requireCurrency(mainCurrency);
        String mainAccountNo = commonService.getNextMainAccount();
        GetLedgerAccountResponseDto main = createAccountUseCase.execute(new CreateLedgerAccountRequestDto(
            "10", "99", "00", "NA", mainAccountNo, "0000", currency, false));
        if (extraCurrencies != null && !extraCurrencies.isEmpty()) {
            createAccountUseCase.executeByAssociatedCurrencies(mainAccountNo, extraCurrencies);
        }
        String oid = ownerId != null ? ownerId : associatedIdentifier;
        return execute(new CreateLedgerWalletRequestDto(
            main.id(), associatedIdentifier, oid, currency, oid));
    }
}
