package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.request.AccountOpenSpecDto;
import com.altech.ledger.entity.dto.request.BatchCreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.response.BatchCreateWalletOnboardResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.CommonService;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wallet create: one wallet + flexible account lines under one numeric main account.
 * <p>
 * COA keys are digit-only ({@link CoaCodes}). Customer identity remains {@code extIdentifier}.
 * Product-line {@code refCode} is free-form (SDK); when numeric it becomes the leaf sub code.
 */
@Component
@RequiredArgsConstructor
public class CreateWalletOnboardingUseCase {
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final CommonService commonService;
    private final CommonUseCase commonUseCase;

    @Transactional
    public GetWalletOnboardResponseDto execute(CreateWalletOnboardRequestDto request) {
        String extIdentifier = request.extIdentifier();
        Currency currency = commonUseCase.requireCurrency(request.currency());
        if (_exists(extIdentifier, currency)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already onboarded: " + extIdentifier + " / " + currency);
        }
        return _createWallet(request);
    }

    /**
     * CRM bulk import — soft-idempotent (existing rows counted, not errors).
     */
    @Transactional
    public BatchCreateWalletOnboardResponseDto executeBatch(BatchCreateWalletOnboardRequestDto request) {
        int created = 0;
        int alreadyExists = 0;
        List<GetWalletOnboardResponseDto> createdWallets = new ArrayList<>();
        List<String> existingExtIdentifiers = new ArrayList<>();

        for (CreateWalletOnboardRequestDto item : request.wallets()) {
            String extIdentifier = item.extIdentifier();
            Currency currency = commonUseCase.requireCurrency(item.currency());
            if (_exists(extIdentifier, currency)) {
                alreadyExists++;
                existingExtIdentifiers.add(extIdentifier);
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
                    existingExtIdentifiers.add(extIdentifier);
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
            .alreadyExistingExtIdentifiers(existingExtIdentifiers)
            .build();
    }

    private boolean _exists(String extIdentifier, Currency currency) {
        return walletRepository.existsByOwnerIdAndCurrency(extIdentifier, currency);
    }

    private GetWalletOnboardResponseDto _createWallet(CreateWalletOnboardRequestDto request) {
        String extIdentifier = request.extIdentifier();
        Currency currency = commonUseCase.requireCurrency(request.currency());
        List<AccountOpenSpecDto> specs = _normalizeAccounts(request.accounts());

        if (walletRepository.existsByOwnerIdAndCurrency(extIdentifier, currency)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already onboarded: " + extIdentifier + " / " + currency);
        }

        String displayName = request.name() == null || request.name().isBlank()
            ? "Wallet " + extIdentifier
            : request.name();

        // One main account number for the whole wallet account-set
        String mainAccount = commonService.getNextMainAccount();
        CoaType coaType = CoaType.LIABILITY;

        Map<String, AccountOpenSpecDto> bySub = new LinkedHashMap<>();
        Map<String, Account> opened = new LinkedHashMap<>();
        Account primary = null;
        Set<String> usedSubs = new HashSet<>();
        int sequential = 0;

        for (AccountOpenSpecDto spec : specs) {
            String sub = _allocateSub(spec, usedSubs, sequential);
            if (!spec.isPrimaryLine() && (spec.refCode() == null || !spec.refCode().matches("\\d{1,4}"))) {
                sequential++;
            }
            usedSubs.add(sub);

            String fullNumber = CoaCodes.fullNumber(mainAccount, sub, coaType, currency);
            if (accountRepository.existsByFullNumber(fullNumber)
                || accountRepository.findByMainAccountAndSubAccount(mainAccount, sub).isPresent()) {
                throw new BizException(AccountErrorResponse.ACC0409, "Account already exists: " + fullNumber);
            }

            boolean allowNegative = Boolean.TRUE.equals(spec.allowNegative());
            Account account = accountRepository.save(new Account(
                fullNumber,
                CoaCodes.ENTITY,
                CoaCodes.typeCode(coaType),
                CoaCodes.SUB_TYPE,
                mainAccount,
                sub,
                CoaCodes.BUFFER,
                currency,
                allowNegative
            ));
            bySub.put(sub, spec);
            opened.put(sub, account);
            if (spec.isPrimaryLine()) {
                primary = account;
            }
        }

        if (primary == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "Primary account is required in accounts");
        }

        String alias = _uniqueAlias(extIdentifier, currency);
        String extType = request.extType() == null || request.extType().isBlank()
            ? "CRM" : request.extType();

        Wallet wallet = walletRepository.save(new Wallet(
            primary.getId(),
            alias,
            displayName,
            extIdentifier,
            extType,
            WalletAssociationType.CUSTODIAN,
            WalletType.INDIVIDUAL,
            WalletStatus.ACTIVE,
            extIdentifier,
            currency));

        List<GetWalletAccountResponseDto> accountDtos = new ArrayList<>();
        for (Map.Entry<String, Account> e : opened.entrySet()) {
            AccountOpenSpecDto spec = bySub.get(e.getKey());
            boolean isPrimary = spec != null && spec.isPrimaryLine();
            String refCode = isPrimary ? null : (spec != null ? spec.refCode() : null);
            // Prefer caller display name when provided; else numeric leaf
            String name = spec != null && spec.name() != null
                ? spec.name()
                : (isPrimary ? displayName : e.getKey());
            accountDtos.add(DtoWrapper.getWalletAccountResponseDto(e.getValue(), refCode, isPrimary, name));
        }
        return DtoWrapper.getWalletOnboardResponseDto(wallet, primary, accountDtos);
    }

    private String _allocateSub(AccountOpenSpecDto spec, Set<String> used, int sequential) {
        if (spec.isPrimaryLine()) {
            return CoaCodes.PRIMARY_SUB;
        }
        String candidate = CoaCodes.subAccountCode(spec.refCode(), sequential + 1);
        if (used.contains(candidate) || CoaCodes.PRIMARY_SUB.equals(candidate)) {
            int n = sequential + 1;
            do {
                n++;
                candidate = CoaCodes.subAccountCode(null, n);
            } while (used.contains(candidate));
        }
        return candidate;
    }

    private List<AccountOpenSpecDto> _normalizeAccounts(List<AccountOpenSpecDto> raw) {
        List<AccountOpenSpecDto> out = new ArrayList<>();
        out.add(AccountOpenSpecDto.primaryLine());
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        LinkedHashMap<String, AccountOpenSpecDto> extra = new LinkedHashMap<>();
        for (AccountOpenSpecDto spec : raw) {
            if (spec == null) {
                continue;
            }
            if (spec.isPrimaryLine()) {
                out.set(0, new AccountOpenSpecDto(null, spec.name(), true, spec.allowNegative()));
                continue;
            }
            if (spec.refCode() == null) {
                continue;
            }
            extra.putIfAbsent(spec.refCode(), spec);
        }
        out.addAll(extra.values());
        return out;
    }

    private String _uniqueAlias(String extIdentifier, Currency currency) {
        String base = extIdentifier + "-" + currency.getIsoCode();
        if (!walletRepository.existsByAlias(base)) {
            return base;
        }
        return base + "-" + System.currentTimeMillis() % 100_000;
    }
}
