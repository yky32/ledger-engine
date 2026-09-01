package com.altech.ledger.usecase.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.AccountOpenSpecDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.po.coa.CoaProfile;
import com.altech.ledger.entity.po.ingest.IngestPolicy;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.coa.CoaBookResolver;
import com.altech.ledger.usecase.coa.CoaProfileUseCase;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolve wallet for ingest: find existing or auto-create from DB {@link IngestPolicy}.
 * COA: event metadata override → Door autoWalletCoaProfileCode → CoaCodes (no DEFAULT profile).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnsureWalletForIngestUseCase {
    private final IngestPolicyUseCase ingestPolicyUseCase;
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;
    private final CoaProfileUseCase coaProfileUseCase;
    private final CoaBookResolver coaBookResolver;

    public record ResolveResult(Wallet wallet, boolean provisioned) {}

    @Transactional
    public ResolveResult resolveOrProvision(String ownerId, Currency pointCurrency) {
        return resolveOrProvision(ownerId, pointCurrency, Map.of());
    }

    @Transactional
    public ResolveResult resolveOrProvision(String ownerId, Currency pointCurrency, Map<String, String> metadata) {
        return resolveOrProvision(ownerId, pointCurrency, metadata, null);
    }

    @Transactional
    public ResolveResult resolveOrProvision(
        String ownerId,
        Currency pointCurrency,
        Map<String, String> metadata,
        String mainAccount
    ) {
        Optional<Wallet> existing = _find(ownerId);
        if (existing.isPresent()) {
            Wallet w = existing.get();
            coaBookResolver.assertMemberMainAccountUsable(w, mainAccount);
            _ensureCurrencyAccount(w, pointCurrency, mainAccount);
            return new ResolveResult(w, false);
        }

        IngestPolicy cfg = ingestPolicyUseCase.requireEffective();
        if (!Boolean.TRUE.equals(cfg.getIsAutoCreateWallet())) {
            return null;
        }

        Currency settlement = Currency.get(
            cfg.getAutoWalletSettlementCurrency() == null ? "HKD" : cfg.getAutoWalletSettlementCurrency());
        Currency ensure = Currency.get(
            cfg.getAutoWalletEnsureCurrency() == null ? "LP" : cfg.getAutoWalletEnsureCurrency());
        if (pointCurrency != null) {
            ensure = pointCurrency;
        }

        String coaCode = resolveCoaProfileCode(cfg, metadata);

        boolean provisioned = false;
        try {
            List<AccountOpenSpecDto> extras = List.of();
            if (ensure != settlement) {
                extras = List.of(new AccountOpenSpecDto(
                    ensure.getIsoCode(),
                    ensure.getIsoCode() + " book",
                    false,
                    false,
                    ensure));
            }
            String name = (cfg.getAutoWalletNamePrefix() == null ? "Auto " : cfg.getAutoWalletNamePrefix())
                + ownerId;

            createWalletOnboardingUseCase.executeIsolated(new CreateWalletOnboardRequestDto(
                ownerId,
                settlement,
                name,
                null,
                coaCode,
                extras,
                mainAccount
            ));
            provisioned = true;
            log.info("auto-created wallet for ownerId={} settlement={} ensure={} coa={} mainAccount={}",
                ownerId, settlement, ensure, coaCode, mainAccount);
        } catch (BizException ex) {
            String code = ex.getResponse() != null ? ex.getResponse().getCode() : null;
            if (WalletErrorResponse.WAL0409.getCode().equals(code)) {
                log.info("auto-create race, wallet already exists for {}", ownerId);
            } else {
                throw ex;
            }
        }

        Wallet wallet = _find(ownerId)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found after auto-create: " + ownerId));
        _ensureCurrencyAccount(wallet, pointCurrency != null ? pointCurrency : ensure, mainAccount);
        return new ResolveResult(wallet, provisioned);
    }

    /**
     * Priority: metadata.coaProfileCode → Door autoWalletCoaProfileCode → null (CoaCodes, no DEFAULT row).
     * Standalone product: no client-specific stream aliases.
     */
    static String resolveCoaProfileCode(IngestPolicy cfg, Map<String, String> metadata) {
        Map<String, String> md = metadata == null ? Map.of() : metadata;
        String fromMeta = firstNonBlank(md.get("coaProfileCode"), md.get("coa_profile_code"));
        if (fromMeta != null) {
            return fromMeta.trim().toUpperCase(Locale.ROOT);
        }
        if (cfg != null && cfg.getAutoWalletCoaProfileCode() != null
            && !cfg.getAutoWalletCoaProfileCode().isBlank()) {
            return cfg.getAutoWalletCoaProfileCode().trim().toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private Optional<Wallet> _find(String ownerId) {
        return walletRepository.findByOwnerId(ownerId);
    }

    /** Chart structure → account under this wallet. Prefer posting via AccountingRule. */
    @Transactional
    public Account ensureAccountForCoa(Wallet wallet, CoaProfile coa) {
        return coaBookResolver.ensureAccount(wallet, coa);
    }

    private void _ensureCurrencyAccount(Wallet wallet, Currency currency, String memberMainAccount) {
        if (currency == null || wallet.getAccountId() == null) {
            return;
        }
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Primary account missing for wallet " + wallet.getId()));
        String main = CoaBookResolver.normalizeMainAccount(memberMainAccount);
        if (main != null && !main.equals(primary.getMainAccount())) {
            // Second product tree is opened by CoaBookResolver at posting time.
            return;
        }
        if (primary.getCurrency() == currency) {
            return;
        }
        boolean exists = accountRepository.findAllByMainAccountAndCurrency(
                primary.getMainAccount(), currency)
            .stream()
            .findAny()
            .isPresent();
        if (exists) {
            return;
        }

        String sub = CoaCodes.subAccountCode(null, 1);
        int n = 1;
        while (accountRepository.findByMainAccountAndSubAccount(primary.getMainAccount(), sub).isPresent()) {
            n++;
            sub = CoaCodes.subAccountCode(null, n);
            if (n > 99) {
                throw new BizException(AccountErrorResponse.ACC0400,
                    "No free sub-account under main " + primary.getMainAccount());
            }
        }
        CoaProfileUseCase.Segments seg = coaProfileUseCase.segments(null);
        String fullNumber = CoaCodes.fullNumber(
            seg.entity(), seg.type(), seg.subType(), primary.getMainAccount(), sub, seg.buffer(), currency);
        accountRepository.save(Account.builder()
            .walletId(wallet.getId())
            .fullNumber(fullNumber)
            .entity(seg.entity())
            .type(seg.type())
            .subType(seg.subType())
            .mainAccount(primary.getMainAccount())
            .subAccount(sub)
            .buffer(seg.buffer())
            .currency(currency)
            .allowNegative(false)
            .build());
        log.info("ensured {} account under wallet {} main={}",
            currency, wallet.getId(), primary.getMainAccount());
    }
}
