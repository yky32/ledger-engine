package com.altech.ledger.usecase.coa;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.po.coa.CoaProfile;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.CoaErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.CoaProfileRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Open or reuse the account whose COA 4-tuple matches a chart profile under a wallet.
 * Member books: movement wallet + optional event {@code mainAccount} (one tree per card number).
 * House books: the company wallet stamped on the HOUSE_* profile (ignores event mainAccount).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoaBookResolver {
    private final CoaProfileRepository coaProfileRepository;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final HouseBooksUseCase houseBooksUseCase;

    /**
     * @param coaCode        {@link CoaProfile#getCode()}
     * @param memberWalletId movement wallet — used when the profile has no house {@code walletId}
     */
    @Transactional
    public Account resolve(String coaCode, Long memberWalletId) {
        return resolve(coaCode, memberWalletId, null);
    }

    /**
     * Member legs use {@code memberMainAccount} (event {@code 9089…}/{@code 9088…}) when present;
     * house legs ignore it and stay on the company wallet primary.
     */
    @Transactional
    public Account resolve(String coaCode, Long memberWalletId, String memberMainAccount) {
        CoaProfile coa = requireProfile(coaCode);
        Wallet wallet = resolveWallet(coa, memberWalletId);
        return ensureAccount(wallet, coa, memberMainAccount);
    }

    /** Member posting book for this currency — never a stored account id. */
    @Transactional
    public Account resolveMemberBook(Long memberWalletId, Currency currency) {
        if (memberWalletId == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "Member wallet required");
        }
        if (currency == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "currency required");
        }
        Optional<CoaProfile> profile = findCustomerCustodian(currency);
        if (profile.isPresent()) {
            return resolve(profile.get().getCode(), memberWalletId);
        }
        Wallet wallet = walletRepository.findById(memberWalletId)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Member wallet not found: " + memberWalletId));
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Primary account missing for wallet " + wallet.getId()));
        if (primary.getCurrency() == currency) {
            return primary;
        }
        return accountRepository.findFirstByMainAccountAndCurrency(primary.getMainAccount(), currency)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "No " + currency + " book on wallet " + memberWalletId));
    }

    @Transactional(readOnly = true)
    public Optional<Account> findMemberBook(Long memberWalletId, Currency currency) {
        if (memberWalletId == null || currency == null) {
            return Optional.empty();
        }
        Optional<CoaProfile> profile = findCustomerCustodian(currency);
        if (profile.isEmpty()) {
            return Optional.empty();
        }
        CoaProfile coa = profile.get();
        String entity = blank(coa.getEntity(), "01");
        String type = blank(coa.getType(), "01");
        String subType = blank(coa.getSubType(), "01");
        Currency ccy = Currency.get(coa.getCurrency() == null ? currency.getIsoCode() : coa.getCurrency());
        return accountRepository.findAllByWalletId(memberWalletId).stream()
            .filter(a -> entity.equals(a.getEntity())
                && type.equals(a.getType())
                && subType.equals(a.getSubType())
                && a.getCurrency() == ccy)
            .findFirst();
    }

    public static String customerCustodianCode(Currency currency) {
        return "CUSTOMER_CUST_" + currency.getIsoCode();
    }

    /** @deprecated use {@link #customerCustodianCode(Currency)} */
    @Deprecated
    public static String memberCustodianCode(Currency currency) {
        return customerCustodianCode(currency);
    }

    @Transactional
    public Account ensureAccount(Wallet wallet, CoaProfile coa) {
        return ensureAccount(wallet, coa, null);
    }

    @Transactional
    public Account ensureAccount(Wallet wallet, CoaProfile coa, String memberMainAccount) {
        if (wallet == null || wallet.getAccountId() == null) {
            throw new BizException(AccountErrorResponse.ACC0404, "Wallet primary missing");
        }
        if (coa == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "COA profile required");
        }
        Currency ccy = Currency.get(coa.getCurrency() == null ? "LP" : coa.getCurrency());
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Primary account missing for wallet " + wallet.getId()));
        String main = memberBookMainAccount(wallet, coa, primary, memberMainAccount);
        String entity = blank(coa.getEntity(), CoaCodes.ENTITY);
        String type = blank(coa.getType(), CoaCodes.typeCodeLiability());
        String subType = blank(coa.getSubType(), CoaCodes.SUB_TYPE);
        Optional<Account> existing = accountRepository
            .findFirstByMainAccountAndEntityAndTypeAndSubTypeAndCurrency(
                main, entity, type, subType, ccy);
        if (existing.isPresent()) {
            Account a = existing.get();
            boolean dirty = false;
            if (a.getWalletId() == null) {
                a.setWalletId(wallet.getId());
                dirty = true;
            }
            if (Boolean.TRUE.equals(coa.getPoolAllowNegative()) && !a.isAllowNegative()) {
                a.setAllowNegative(true);
                dirty = true;
            }
            return dirty ? accountRepository.save(a) : a;
        }
        String buffer = blank(coa.getBuffer(), CoaCodes.BUFFER);
        String fullNumber = CoaCodes.fullNumber(
            entity, type, subType, main, buffer, ccy);
        Account created = accountRepository.save(Account.builder()
            .walletId(wallet.getId())
            .fullNumber(fullNumber)
            .entity(entity)
            .type(type)
            .subType(subType)
            .mainAccount(main)
            .buffer(buffer)
            .currency(ccy)
            .allowNegative(Boolean.TRUE.equals(coa.getPoolAllowNegative()))
            .build());
        log.info("COA account wallet={} coa={} mainAccount={} fullNumber={}",
            wallet.getId(), coa.getCode(), main, fullNumber);
        return created;
    }

    /** Reject a client mainAccount that already belongs to another wallet. Blank is a no-op. */
    public void assertMemberMainAccountUsable(Wallet wallet, String memberMainAccount) {
        String requested = normalizeMainAccount(memberMainAccount);
        if (requested == null || wallet == null) {
            return;
        }
        for (Account a : accountRepository.findAllByMainAccount(requested)) {
            if (a.getWalletId() != null && wallet.getId() != null
                && !a.getWalletId().equals(wallet.getId())) {
                throw new BizException(AccountErrorResponse.ACC0409,
                    "mainAccount already in use: " + requested);
            }
        }
    }

    public static String normalizeMainAccount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    private String memberBookMainAccount(
        Wallet wallet,
        CoaProfile coa,
        Account primary,
        String memberMainAccount
    ) {
        if (coa.getWalletId() != null || CoaCodes.isHouseCode(coa.getCode())) {
            return primary.getMainAccount();
        }
        String requested = normalizeMainAccount(memberMainAccount);
        if (requested == null) {
            return primary.getMainAccount();
        }
        assertMemberMainAccountUsable(wallet, requested);
        return requested;
    }

    private Wallet resolveWallet(CoaProfile coa, Long memberWalletId) {
        if (coa.getWalletId() != null) {
            return walletRepository.findById(coa.getWalletId())
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                    "House wallet missing for COA " + coa.getCode() + " walletId=" + coa.getWalletId()));
        }
        if (CoaCodes.isHouseCode(coa.getCode())) {
            houseBooksUseCase.ensure(HouseBooksUseCase.DEFAULT_OWNER);
            CoaProfile reloaded = requireProfile(coa.getCode());
            if (reloaded.getWalletId() != null) {
                return walletRepository.findById(reloaded.getWalletId())
                    .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                        "House wallet missing after ensure for COA " + coa.getCode()));
            }
        }
        if (memberWalletId == null) {
            throw new BizException(AccountErrorResponse.ACC0400,
                "Member wallet required to resolve COA " + coa.getCode());
        }
        return walletRepository.findById(memberWalletId)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Member wallet not found: " + memberWalletId));
    }

    private Optional<CoaProfile> findCustomerCustodian(Currency currency) {
        String neo = customerCustodianCode(currency);
        Optional<CoaProfile> hit = enabledByCode(neo);
        if (hit.isPresent()) {
            return hit;
        }
        return enabledByCode("MEMBER_CUST_" + currency.getIsoCode());
    }

    private Optional<CoaProfile> enabledByCode(String code) {
        return coaProfileRepository.findByCode(code)
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && Boolean.TRUE.equals(p.getIsEnabled()));
    }

    private CoaProfile requireProfile(String coaCode) {
        if (coaCode == null || coaCode.isBlank()) {
            throw new BizException(CoaErrorResponse.COA0400, "AccountingRule.targetAccount (CoaProfile.code) required");
        }
        String code = coaCode.trim().toUpperCase(Locale.ROOT);
        Optional<CoaProfile> hit = enabledByCode(code);
        if (hit.isEmpty() && code.startsWith("MEMBER_CUST_")) {
            hit = enabledByCode("CUSTOMER_CUST_" + code.substring("MEMBER_CUST_".length()));
        }
        if (hit.isEmpty() && code.startsWith("CUSTOMER_CUST_")) {
            hit = enabledByCode("MEMBER_CUST_" + code.substring("CUSTOMER_CUST_".length()));
        }
        return hit.orElseThrow(() -> new BizException(CoaErrorResponse.COA0404, "code=" + code));
    }

    private static String blank(String v, String d) {
        return v == null || v.isBlank() ? d : v.trim();
    }
}
