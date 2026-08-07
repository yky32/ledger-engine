package com.altech.ledger.usecase.setup;

import com.altech.ledger.entity.dto.parity.LedgerAccountDtos;
import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.CommonService;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.account.WalletAccountBalanceUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletSetupUseCase {
    private final AccountRepository accounts;
    private final WalletRepository wallets;
    private final AccountSetupUseCase accountSetupUseCase;
    private final CommonService commonService;
    private final WalletAccountBalanceUseCase balanceUseCase;

    /**
     * Full wallet create like legacy associatedWithAccountsCreation + create:
     * main account (+ optional multi-ccy) then wallet.
     */
    @Transactional
    public LedgerWalletDtos.WithBalancesResponse createFull(String ownerId, String mainCurrency,
                                                 List<String> extraCurrencies,
                                                 String extIdentifier, String extType) {
        String mainAccountNo = commonService.getNextMainAccount();
        LedgerAccountDtos.Response main = accountSetupUseCase.create(new LedgerAccountDtos.CreateRequest(
            "10", "99", "00", "NA", mainAccountNo, "0000", mainCurrency.toUpperCase(), false));
        if (extraCurrencies != null && !extraCurrencies.isEmpty()) {
            accountSetupUseCase.createByAssociatedCurrencies(mainAccountNo, extraCurrencies);
        }
        return create(new LedgerWalletDtos.CreateRequest(
            main.id(), extIdentifier, extType, WalletAssociationType.CUSTODIAN,
            ownerId, mainCurrency.toUpperCase(), ownerId == null ? "NA" : ownerId));
    }

    @Transactional
    public LedgerWalletDtos.WithBalancesResponse create(LedgerWalletDtos.CreateRequest dto) {
        Account account = accounts.findById(dto.accountId())
            .orElseThrow(() -> LedgerException.notFound("Account not found: " + dto.accountId()));

        String alias = nextAlias();
        String currency = dto.currency() != null ? dto.currency().toUpperCase() : account.getCurrency();
        String ownerId = dto.ownerId() != null ? dto.ownerId()
            : (dto.extIdentifier() != null ? dto.extIdentifier() : alias);
        String nickname = dto.nickname() == null || dto.nickname().isBlank() ? "NA" : dto.nickname();
        WalletAssociationType type = dto.type() == null ? WalletAssociationType.CUSTODIAN : dto.type();

        if (wallets.existsByOwnerIdAndCurrency(ownerId, currency)) {
            throw LedgerException.conflict("WALLET_EXISTS",
                "Wallet already exists for owner/currency: " + ownerId + "/" + currency);
        }

        Wallet wallet = new Wallet(
            account.getId(), alias, nickname, dto.extIdentifier(), dto.extType(),
            type, WalletType.CORPORATE, WalletStatus.PENDING, ownerId, currency);
        wallet = wallets.save(wallet);
        List<Account> myAccounts = new ArrayList<>(accounts.findAllByMainAccount(account.getMainAccount()));
        return DtoMapper.toWallet(wallet, myAccounts);
    }

    @Transactional
    public LedgerWalletDtos.WithBalancesResponse activate(Long id, LedgerWalletDtos.ActivationRequest ignored) {
        // Standalone: no IDV client — activate immediately (parity endpoint retained)
        return markActive(id);
    }

    @Transactional
    public LedgerWalletDtos.WithBalancesResponse markActive(Long id) {
        Wallet wallet = wallet(id);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallets.save(wallet);
        return balanceUseCase.getOne(wallet.getId(), null);
    }

    @Transactional
    public LedgerWalletDtos.WithBalancesResponse update(Long id, LedgerWalletDtos.UpdateRequest dto) {
        Wallet wallet = wallet(id);
        if (dto.status() != null) wallet.setStatus(dto.status());
        if (dto.accountId() != null) wallet.setAccountId(dto.accountId());
        if (dto.extIdentifier() != null) wallet.setExtIdentifier(dto.extIdentifier());
        if (dto.type() != null) wallet.setType(dto.type());
        if (dto.nickname() != null) wallet.setNickname(dto.nickname());
        wallets.save(wallet);
        return balanceUseCase.getOne(wallet.getId(), null);
    }

    private Wallet wallet(Long id) {
        return wallets.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Wallet not found: " + id));
    }

    private String nextAlias() {
        return String.valueOf(System.currentTimeMillis() % 1_000_000_000L)
            + (UUID.randomUUID().toString().replace("-", "").substring(0, 4));
    }
}
