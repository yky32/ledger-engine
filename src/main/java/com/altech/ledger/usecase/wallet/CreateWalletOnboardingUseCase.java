package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
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
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.usecase.ledger.CreateLedgerAccountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wallet create: one wallet + flexible account-set.
 * <p>
 * Customer identity is {@code extIdentifier}. Account lines are free-form
 * ({@code refCode}) — product catalogs live in the client / SDK, not this core.
 */
@Component
@RequiredArgsConstructor
public class CreateWalletOnboardingUseCase {
    private final IntegrationProperties integrationProperties;
    private final CreateLedgerAccountUseCase createLedgerAccountUseCase;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
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

    public String walletRef(String extIdentifier, Currency currency) {
        Currency ccy = commonUseCase.requireCurrency(currency);
        String id = extIdentifier == null ? "" : extIdentifier;
        return integrationProperties.getWalletRefTemplate()
            .replace("{extIdentifier}", id)
            .replace("{currency}", ccy.getIsoCode());
    }

    public String walletRef(String extIdentifier, String currency) {
        return walletRef(extIdentifier, commonUseCase.requireCurrency(currency));
    }

    /**
     * Account fullNumber: primary → base ref; others → base:refCode.
     */
    public String accountRef(String extIdentifier, Currency currency, AccountOpenSpecDto spec) {
        String base = walletRef(extIdentifier, currency);
        if (spec.isPrimaryLine()) {
            return base;
        }
        return base + ":" + spec.refCode();
    }

    private boolean _exists(String extIdentifier, Currency currency) {
        if (walletRepository.existsByOwnerIdAndCurrency(extIdentifier, currency)) {
            return true;
        }
        return accountRepository.existsByFullNumber(walletRef(extIdentifier, currency));
    }

    private GetWalletOnboardResponseDto _createWallet(CreateWalletOnboardRequestDto request) {
        String extIdentifier = request.extIdentifier();
        Currency currency = commonUseCase.requireCurrency(request.currency());
        String baseRef = walletRef(extIdentifier, currency);
        List<AccountOpenSpecDto> specs = _normalizeAccountSet(request.accountSet());

        if (walletRepository.existsByOwnerIdAndCurrency(extIdentifier, currency)
            || accountRepository.existsByFullNumber(baseRef)) {
            throw new BizException(WalletErrorResponse.WAL0409,
                "Wallet already onboarded: " + extIdentifier + " / " + currency);
        }

        String name = request.name() == null || request.name().isBlank()
            ? "Wallet " + extIdentifier
            : request.name();

        // key: fullNumber
        Map<String, AccountOpenSpecDto> byRef = new LinkedHashMap<>();
        Map<String, Account> opened = new LinkedHashMap<>();
        Account primary = null;

        for (AccountOpenSpecDto spec : specs) {
            String ref = accountRef(extIdentifier, currency, spec);
            if (byRef.containsKey(ref)) {
                continue;
            }
            byRef.put(ref, spec);
            if (accountRepository.existsByFullNumber(ref)) {
                throw new BizException(AccountErrorResponse.ACC0409, "Account already exists: " + ref);
            }
            String accountName = name + " / " + spec.label();
            boolean allowNegative = Boolean.TRUE.equals(spec.allowNegative());
            createLedgerAccountUseCase.execute(new CreateAccountRequest(
                ref, accountName, CoaType.LIABILITY, currency, allowNegative));
            Account account = accountRepository.findByFullNumber(ref)
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account missing after create: " + ref));
            opened.put(ref, account);
            if (spec.isPrimaryLine()) {
                primary = account;
            }
        }

        if (primary == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "Primary account is required in accountSet");
        }

        String alias = _uniqueAlias(extIdentifier, currency);
        String extType = request.extType() == null || request.extType().isBlank()
            ? "CRM" : request.extType();

        Wallet wallet = walletRepository.save(new Wallet(
            primary.getId(),
            alias,
            name,
            extIdentifier,
            extType,
            WalletAssociationType.CUSTODIAN,
            WalletType.INDIVIDUAL,
            WalletStatus.ACTIVE,
            extIdentifier,
            currency));

        List<GetWalletAccountResponseDto> accountDtos = new ArrayList<>();
        for (Map.Entry<String, Account> e : opened.entrySet()) {
            AccountOpenSpecDto spec = byRef.get(e.getKey());
            accountDtos.add(DtoWrapper.getWalletAccountResponseDto(
                e.getValue(),
                spec != null && !spec.isPrimaryLine() ? spec.refCode() : null,
                spec != null && spec.isPrimaryLine()));
        }
        return DtoWrapper.getWalletOnboardResponseDto(wallet, primary, accountDtos);
    }

    /**
     * Ensure primary first; dedupe by account fullNumber key (via refCode).
     * Empty → primary only. Product-line codes are opaque strings.
     */
    private List<AccountOpenSpecDto> _normalizeAccountSet(List<AccountOpenSpecDto> raw) {
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
                // allow caller to override primary allowNegative / name
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
