package com.altech.ledger.usecase.wallet;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.entity.dto.request.BatchCreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.response.BatchCreateWalletOnboardResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase-1 wallet create (TGT: {@code Create*UseCase.execute}).
 */
@Service
@RequiredArgsConstructor
public class CreateWalletOnboardingUseCase {
    private final IntegrationProperties properties;
    private final LedgerUseCase ledgerUseCase;
    private final AccountRepository accounts;
    private final WalletRepository wallets;

    @Transactional
    public GetWalletOnboardResponseDto execute(CreateWalletOnboardRequestDto request) {
        if (exists(request.userId(), request.currency())) {
            throw new BizException(WalletErrorResponse.WAL0409, "Wallet already onboarded: " + request.userId() + " / " + request.currency());
        }
        return createWallet(request);
    }

    /**
     * CRM bulk import — soft-idempotent (existing rows counted, not errors).
     */
    @Transactional
    public BatchCreateWalletOnboardResponseDto executeBatch(BatchCreateWalletOnboardRequestDto request) {
        int created = 0;
        int alreadyExists = 0;
        List<GetWalletOnboardResponseDto> createdWallets = new ArrayList<>();
        List<String> existingUserIds = new ArrayList<>();

        for (CreateWalletOnboardRequestDto item : request.wallets()) {
            if (exists(item.userId(), item.currency())) {
                alreadyExists++;
                existingUserIds.add(item.userId());
                continue;
            }
            try {
                createdWallets.add(createWallet(item));
                created++;
            } catch (BizException ex) {
                String code = ex.getResponse() != null ? ex.getResponse().getCode() : null;
                if (WalletErrorResponse.WAL0409.getCode().equals(code)
                    || AccountErrorResponse.ACC0409.getCode().equals(code)) {
                    alreadyExists++;
                    existingUserIds.add(item.userId());
                } else {
                    throw ex;
                }
            }
        }

        return BatchCreateWalletOnboardResponseDto.builder()
            .requested(request.wallets().size())
            .created(created)
            .alreadyExists(alreadyExists)
            .createdWallets(createdWallets)
            .alreadyExistingUserIds(existingUserIds)
            .build();
    }

    public String walletRef(String userId, String currency) {
        String ccy = currency == null ? "" : currency.trim().toUpperCase();
        return properties.getWalletRefTemplate()
            .replace("{userId}", userId == null ? "" : userId)
            .replace("{currency}", ccy);
    }

    private boolean exists(String userId, String currency) {
        String ccy = currency == null ? null : currency.trim().toUpperCase();
        if (wallets.existsByOwnerIdAndCurrency(userId, ccy)) {
            return true;
        }
        return accounts.existsByFullNumber(walletRef(userId, ccy));
    }

    private GetWalletOnboardResponseDto createWallet(CreateWalletOnboardRequestDto request) {
        String userId = request.userId();
        String currency = request.currency();
        String externalReference = walletRef(userId, currency);

        if (wallets.existsByOwnerIdAndCurrency(userId, currency)
            || accounts.existsByFullNumber(externalReference)) {
            throw new BizException(WalletErrorResponse.WAL0409, "Wallet already onboarded: " + userId + " / " + currency);
        }

        String name = request.name() == null || request.name().isBlank()
            ? "Wallet " + userId
            : request.name();

        ledgerUseCase.createAccount(new CreateAccountRequest(
            externalReference, name, CoaType.LIABILITY, currency, false));

        Account account = accounts.findByFullNumber(externalReference)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account missing after create: " + externalReference));

        String alias = uniqueAlias(userId, currency);
        String extId = request.externalId() == null || request.externalId().isBlank()
            ? userId : request.externalId();
        String extType = request.externalType() == null || request.externalType().isBlank()
            ? "CRM" : request.externalType();

        Wallet wallet = wallets.save(new Wallet(
            account.getId(),
            alias,
            name,
            extId,
            extType,
            WalletAssociationType.CUSTODIAN,
            WalletType.INDIVIDUAL,
            WalletStatus.ACTIVE,
            userId,
            currency));

        return DtoWrapper.getWalletOnboardResponseDto(wallet, account);
    }

    private String uniqueAlias(String userId, String currency) {
        String base = userId + "-" + currency;
        if (!wallets.existsByAlias(base)) {
            return base;
        }
        return base + "-" + System.currentTimeMillis() % 100_000;
    }
}
