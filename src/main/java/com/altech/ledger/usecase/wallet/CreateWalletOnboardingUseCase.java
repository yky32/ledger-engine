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
import com.altech.ledger.util.WalletVanityCodes;
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
 * Wallet create: <b>1 ownerId → 1 Wallet</b>.
 * <p>
 * Opens a primary account in {@code settlementCurrency}, plus optional extra accounts
 * (e.g. LP) under the same wallet main COA — multi-currency books, single wallet identity.
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
        String ownerId = request.ownerId();
        if (_exists(ownerId)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already onboarded for customer: " + ownerId);
        }
        return _createWallet(request);
    }

    /** CRM bulk import — soft-idempotent (existing rows counted, not errors). */
    @Transactional
    public BatchCreateWalletOnboardResponseDto executeBatch(BatchCreateWalletOnboardRequestDto request) {
        int created = 0;
        int alreadyExists = 0;
        List<GetWalletOnboardResponseDto> createdWallets = new ArrayList<>();
        List<String> existingOwnerIds = new ArrayList<>();

        for (CreateWalletOnboardRequestDto item : request.wallets()) {
            String ownerId = item.ownerId();
            if (_exists(ownerId)) {
                alreadyExists++;
                existingOwnerIds.add(ownerId);
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
                    existingOwnerIds.add(ownerId);
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
            .alreadyExistingOwnerIds(existingOwnerIds)
            .build();
    }

    private boolean _exists(String ownerId) {
        return walletRepository.existsByOwnerId(ownerId);
    }

    private GetWalletOnboardResponseDto _createWallet(CreateWalletOnboardRequestDto request) {
        String ownerId = request.ownerId();
        Currency settlement = commonUseCase.requireCurrency(request.settlementCurrency());

        if (walletRepository.existsByOwnerId(ownerId)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already onboarded for customer: " + ownerId);
        }

        String displayName = request.name() == null || request.name().isBlank()
            ? "Wallet " + ownerId
            : request.name();

        List<AccountOpenSpecDto> specs = _normalizeAccounts(request.accounts(), settlement);
        String mainAccount = commonService.getNextMainAccount();
        CoaType coaType = CoaType.LIABILITY;

        Map<String, AccountOpenSpecDto> byKey = new LinkedHashMap<>();
        Map<String, Account> opened = new LinkedHashMap<>();
        Account primary = null;
        Set<String> usedSubs = new HashSet<>();
        Set<Currency> usedCurrencies = new HashSet<>();
        int sequential = 0;

        for (AccountOpenSpecDto spec : specs) {
            Currency accountCcy = spec.isPrimaryLine()
                ? settlement
                : commonUseCase.requireCurrency(spec.currency() != null ? spec.currency() : settlement);

            if (!usedCurrencies.add(accountCcy) && !spec.isPrimaryLine()) {
                // one balance row per currency under the wallet for this product path
                continue;
            }

            String sub = _allocateSub(spec, usedSubs, sequential);
            if (!spec.isPrimaryLine() && (spec.refCode() == null || !spec.refCode().matches("\\d{1,4}"))) {
                sequential++;
            }
            usedSubs.add(sub);

            String fullNumber = CoaCodes.fullNumber(mainAccount, sub, coaType, accountCcy);
            if (accountRepository.existsByFullNumber(fullNumber)
                || accountRepository.findByMainAccountAndSubAccount(mainAccount, sub).isPresent()) {
                throw new BizException(AccountErrorResponse.ACC0409, "Account already exists: " + fullNumber);
            }

            boolean allowNegative = Boolean.TRUE.equals(spec.allowNegative());
            Account account = accountRepository.save(Account.builder()
                .fullNumber(fullNumber)
                .entity(CoaCodes.ENTITY)
                .type(CoaCodes.typeCode(coaType))
                .subType(CoaCodes.SUB_TYPE)
                .mainAccount(mainAccount)
                .subAccount(sub)
                .buffer(CoaCodes.BUFFER)
                .currency(accountCcy)
                .allowNegative(allowNegative)
                .build());

            String key = sub + ":" + accountCcy.getIsoCode();
            byKey.put(key, spec);
            opened.put(key, account);
            if (spec.isPrimaryLine()) {
                primary = account;
                usedCurrencies.add(accountCcy);
            }
        }

        if (primary == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "Primary account is required");
        }

        Wallet wallet = new Wallet();
        wallet.setAccountId(primary.getId());
        wallet.setName(displayName);
        wallet.setType(WalletAssociationType.CUSTODIAN);
        wallet.setWalletType(WalletType.INDIVIDUAL);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setOwnerId(ownerId);
        wallet.setVanityCode(WalletVanityCodes.resolveForCreate(request.vanityCode(), ownerId));
        wallet.setSettlementCurrency(settlement);
        wallet = walletRepository.save(wallet);

        List<GetWalletAccountResponseDto> accountDtos = new ArrayList<>();
        for (Map.Entry<String, Account> e : opened.entrySet()) {
            AccountOpenSpecDto spec = byKey.get(e.getKey());
            boolean isPrimary = spec != null && spec.isPrimaryLine();
            String refCode = isPrimary ? null : (spec != null
                ? (spec.refCode() != null ? spec.refCode()
                    : (spec.currency() != null ? spec.currency().getIsoCode() : null))
                : null);
            String name = spec != null && spec.name() != null
                ? spec.name()
                : (isPrimary ? displayName : (spec != null ? spec.label() : e.getKey()));
            accountDtos.add(DtoWrapper.getWalletAccountResponseDto(e.getValue(), refCode, isPrimary, name));
        }
        return DtoWrapper.getWalletOnboardResponseDto(wallet, primary, accountDtos);
    }

    /**
     * Always include primary (settlement). Merge caller extras; skip duplicate primary / settlement ccy.
     */
    private List<AccountOpenSpecDto> _normalizeAccounts(List<AccountOpenSpecDto> raw, Currency settlement) {
        List<AccountOpenSpecDto> out = new ArrayList<>();
        out.add(new AccountOpenSpecDto(null, null, true, false, settlement));
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        Set<String> seenCcy = new HashSet<>();
        seenCcy.add(settlement.getIsoCode());
        for (AccountOpenSpecDto spec : raw) {
            if (spec == null || spec.isPrimaryLine()) {
                continue;
            }
            Currency ccy = spec.currency();
            if (ccy == null && spec.refCode() != null) {
                try {
                    ccy = Currency.get(spec.refCode());
                } catch (Exception ignored) {
                    ccy = null;
                }
            }
            if (ccy == null) {
                continue;
            }
            ccy = commonUseCase.requireCurrency(ccy);
            if (!seenCcy.add(ccy.getIsoCode())) {
                continue;
            }
            String ref = spec.refCode() != null ? spec.refCode() : ccy.getIsoCode();
            String name = spec.name() != null ? spec.name() : ccy.getIsoCode();
            out.add(new AccountOpenSpecDto(ref, name, false, spec.allowNegative(), ccy));
        }
        return out;
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
}
