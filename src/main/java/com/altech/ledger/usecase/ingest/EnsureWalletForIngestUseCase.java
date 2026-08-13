package com.altech.ledger.usecase.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.AccountOpenSpecDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.po.ingest.IngestPolicy;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.coa.CoaProfileUseCase;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Resolve wallet for ingest: find existing or auto-create from DB {@link IngestPolicy}.
 * Extra currency books use default {@code coa_profile} segments.
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

    public record ResolveResult(Wallet wallet, boolean provisioned) {}

    @Transactional
    public ResolveResult resolveOrProvision(String ownerId, Currency pointCurrency) {
        Optional<Wallet> existing = _find(ownerId);
        if (existing.isPresent()) {
            Wallet w = existing.get();
            _ensureCurrencyAccount(w, pointCurrency);
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

            createWalletOnboardingUseCase.execute(new CreateWalletOnboardRequestDto(
                ownerId,
                settlement,
                name,
                extras
            ));
            provisioned = true;
            log.info("auto-created wallet for ownerId={} settlement={} ensure={}",
                ownerId, settlement, ensure);
        } catch (BizException ex) {
            String code = ex.getResponse() != null ? ex.getResponse().getCode() : null;
            if (WalletErrorResponse.WAL0409.getCode().equals(code)
                || AccountErrorResponse.ACC0409.getCode().equals(code)) {
                log.info("auto-create race, wallet already exists for {}", ownerId);
            } else {
                throw ex;
            }
        }

        Wallet wallet = _find(ownerId)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found after auto-create: " + ownerId));
        _ensureCurrencyAccount(wallet, pointCurrency != null ? pointCurrency : ensure);
        return new ResolveResult(wallet, provisioned);
    }

    private Optional<Wallet> _find(String ownerId) {
        return walletRepository.findByOwnerId(ownerId);
    }

    private void _ensureCurrencyAccount(Wallet wallet, Currency currency) {
        if (currency == null || wallet.getAccountId() == null) {
            return;
        }
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Primary account missing for wallet " + wallet.getId()));
        if (primary.getCurrency() == currency) {
            return;
        }
        Optional<Account> existing = accountRepository.findByMainAccountAndCurrency(
            primary.getMainAccount(), currency);
        if (existing.isPresent()) {
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
        CoaProfileUseCase.Segments seg = coaProfileUseCase.segments(wallet.getCoaProfileCode());
        String fullNumber = CoaCodes.fullNumber(
            seg.entity(), seg.type(), seg.subType(), primary.getMainAccount(), sub, seg.buffer(), currency);
        accountRepository.save(Account.builder()
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
        log.info("ensured {} account under wallet {} main={}", currency, wallet.getId(), primary.getMainAccount());
    }
}
