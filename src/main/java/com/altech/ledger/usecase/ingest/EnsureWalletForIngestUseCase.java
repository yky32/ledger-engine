package com.altech.ledger.usecase.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.AccountOpenSpecDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.po.coa.CoaProfile;
import com.altech.ledger.entity.po.ingest.IngestPolicy;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.coa.CoaBookResolver;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolve wallet for ingest: 1 ownerId → 1 wallet.
 * Wallet.settlementCurrency = Door autoWalletSettlementCurrency (HKD).
 * Opens 01-01-01 books on event.mainAccount for settlement + ensure (HKD + LP).
 * Not 10-20-00.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnsureWalletForIngestUseCase {
    private final IngestPolicyUseCase ingestPolicyUseCase;
    private final WalletRepository walletRepository;
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;
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
            return new ResolveResult(w, false);
        }

        IngestPolicy cfg = ingestPolicyUseCase.requireEffective();
        if (!Boolean.TRUE.equals(cfg.getIsAutoCreateWallet())) {
            return null;
        }

        Currency settlement = _settlement(cfg);
        Currency ensure = _ensure(cfg);
        String coaCode = resolveCoaProfileCode(cfg, metadata);
        List<AccountOpenSpecDto> extras = (ensure != null && ensure != settlement)
            ? List.of(AccountOpenSpecDto.ofCurrency(ensure))
            : List.of();

        boolean provisioned = false;
        try {
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
        return new ResolveResult(wallet, provisioned);
    }

    static Currency _settlement(IngestPolicy cfg) {
        String iso = cfg == null || cfg.getAutoWalletSettlementCurrency() == null
            || cfg.getAutoWalletSettlementCurrency().isBlank()
            ? "HKD" : cfg.getAutoWalletSettlementCurrency();
        return Currency.get(iso);
    }

    static Currency _ensure(IngestPolicy cfg) {
        String iso = cfg == null || cfg.getAutoWalletEnsureCurrency() == null
            || cfg.getAutoWalletEnsureCurrency().isBlank()
            ? "LP" : cfg.getAutoWalletEnsureCurrency();
        return Currency.get(iso);
    }

    /**
     * Priority: metadata.coaProfileCode → Door autoWalletCoaProfileCode → null
     * (caller uses CUSTOMER_CUST_{ccy} = 01-01-01).
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
}
