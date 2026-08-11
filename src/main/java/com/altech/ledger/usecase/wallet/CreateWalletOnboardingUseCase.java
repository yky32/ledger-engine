package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.BatchCreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.response.BatchCreateWalletOnboardResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.AccountSet;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Wallet create / upsert: <b>1 CUST → 1 Wallet</b> + default AccountSet + HKD/LP CoA (Phase A).
 */
@Component
public class CreateWalletOnboardingUseCase {
    private final WalletRepository walletRepository;
    private final CommonUseCase commonUseCase;
    private final DefaultAccountSetInitializer defaultAccountSetInitializer;
    private final QueryWalletUseCase queryWalletUseCase;

    public CreateWalletOnboardingUseCase(
        WalletRepository walletRepository,
        CommonUseCase commonUseCase,
        DefaultAccountSetInitializer defaultAccountSetInitializer,
        @Lazy QueryWalletUseCase queryWalletUseCase
    ) {
        this.walletRepository = walletRepository;
        this.commonUseCase = commonUseCase;
        this.defaultAccountSetInitializer = defaultAccountSetInitializer;
        this.queryWalletUseCase = queryWalletUseCase;
    }

    @Transactional
    public GetWalletOnboardResponseDto execute(CreateWalletOnboardRequestDto request) {
        String associatedIdentifier = request.associatedIdentifier();
        if (_exists(associatedIdentifier)) {
            return _upsertExisting(associatedIdentifier, request);
        }
        return _createWallet(request);
    }

    @Transactional
    public BatchCreateWalletOnboardResponseDto executeBatch(BatchCreateWalletOnboardRequestDto request) {
        int created = 0;
        int alreadyExists = 0;
        List<GetWalletOnboardResponseDto> createdWallets = new ArrayList<>();
        List<String> existingAssociatedIdentifiers = new ArrayList<>();

        for (CreateWalletOnboardRequestDto item : request.wallets()) {
            String associatedIdentifier = item.associatedIdentifier();
            if (_exists(associatedIdentifier)) {
                alreadyExists++;
                existingAssociatedIdentifiers.add(associatedIdentifier);
                continue;
            }
            try {
                createdWallets.add(_createWallet(item));
                created++;
            } catch (BizException ex) {
                String code = ex.getResponse() != null ? ex.getResponse().getCode() : null;
                if (WalletErrorResponse.WAL0409.getCode().equals(code)
                    || AccountErrorResponse.ACC0409.getCode().equals(code)) {
                    alreadyExists++;
                    existingAssociatedIdentifiers.add(associatedIdentifier);
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
            .alreadyExistingAssociatedIdentifiers(existingAssociatedIdentifiers)
            .build();
    }

    private boolean _exists(String associatedIdentifier) {
        return walletRepository.existsByOwnerId(associatedIdentifier);
    }

    private GetWalletOnboardResponseDto _upsertExisting(String associatedIdentifier, CreateWalletOnboardRequestDto request) {
        Wallet wallet = walletRepository.findByOwnerId(associatedIdentifier)
            .orElseGet(() -> walletRepository.findByAssociatedIdentifier(associatedIdentifier).stream().findFirst()
                .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "Wallet not found: " + associatedIdentifier)));
        boolean dirty = false;
        if (request.name() != null && !request.name().isBlank() && !request.name().equals(wallet.getNickname())) {
            wallet.setNickname(request.name().trim());
            dirty = true;
        }
        if (request.associatedFrom() != null && !request.associatedFrom().isBlank()
            && !request.associatedFrom().equals(wallet.getAssociatedFrom())) {
            wallet.setAssociatedFrom(request.associatedFrom().trim());
            dirty = true;
        }
        if (dirty) {
            walletRepository.save(wallet);
        }
        return queryWalletUseCase.byAssociatedIdentifier(associatedIdentifier);
    }

    private GetWalletOnboardResponseDto _createWallet(CreateWalletOnboardRequestDto request) {
        String associatedIdentifier = request.associatedIdentifier();
        Currency settlement = commonUseCase.requireCurrency(
            request.settlementCurrency() != null ? request.settlementCurrency() : Currency.HKD);

        if (walletRepository.existsByOwnerId(associatedIdentifier)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already onboarded for customer: " + associatedIdentifier);
        }

        String displayName = request.name() == null || request.name().isBlank()
            ? "Wallet " + associatedIdentifier
            : request.name();

        DefaultAccountSetInitializer.InitializedCoa coa = defaultAccountSetInitializer.openAccounts(settlement);
        Account primary = coa.primary();

        String alias = _uniqueAlias(associatedIdentifier);
        String associatedFrom = request.associatedFrom() == null || request.associatedFrom().isBlank()
            ? "CRM" : request.associatedFrom();

        Wallet wallet = new Wallet();
        wallet.setAccountId(primary.getId());
        wallet.setAlias(alias);
        wallet.setNickname(displayName);
        wallet.setAssociatedIdentifier(associatedIdentifier);
        wallet.setAssociatedFrom(associatedFrom);
        wallet.setType(WalletAssociationType.CUSTODIAN);
        wallet.setWalletType(WalletType.INDIVIDUAL);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setOwnerId(associatedIdentifier);
        wallet.setSettlementCurrency(settlement);
        wallet = walletRepository.save(wallet);

        AccountSet set = defaultAccountSetInitializer.attachDefaultSet(wallet.getId(), coa.accounts());

        List<GetWalletAccountResponseDto> accountDtos = new ArrayList<>();
        for (Account a : coa.accounts()) {
            boolean isPrimary = a.getId().equals(primary.getId());
            String ref = a.getAccountRole() == null ? null : a.getAccountRole().name();
            String name = a.getDisplayName() != null ? a.getDisplayName() : ref;
            accountDtos.add(DtoWrapper.getWalletAccountResponseDto(a, ref, isPrimary, name));
        }
        GetWalletOnboardResponseDto dto = DtoWrapper.getWalletOnboardResponseDto(wallet, primary, accountDtos);
        dto.setAccountSets(List.of(DtoWrapper.getAccountSetResponseDto(set, accountDtos)));
        return dto;
    }

    private String _uniqueAlias(String associatedIdentifier) {
        String base = associatedIdentifier;
        if (!walletRepository.existsByAlias(base)) {
            return base;
        }
        return base + "-" + System.currentTimeMillis() % 100_000;
    }
}
