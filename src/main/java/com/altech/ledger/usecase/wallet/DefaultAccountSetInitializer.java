package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.enu.AccountRole;
import com.altech.ledger.entity.enu.AccountStatus;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.AccountSet;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.AccountSetRepository;
import com.altech.ledger.service.CommonService;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase A: default HKD + LP Chart of Accounts under one {@link AccountSet#CODE_DEFAULT}.
 */
@Component
@RequiredArgsConstructor
public class DefaultAccountSetInitializer {
    private final AccountRepository accountRepository;
    private final AccountSetRepository accountSetRepository;
    private final CommonService commonService;

    public record CoaSlot(Currency currency, AccountRole role, String subAccount, String displayName) {}

    public record InitializedCoa(
        String mainAccount,
        Account primary,
        List<Account> accounts,
        Map<String, Account> byCcyRole
    ) {}

    /** Default loyalty + money template (settlement HKD assumed for primary). */
    public static List<CoaSlot> defaultSlots(Currency settlement) {
        Currency settle = settlement == null ? Currency.HKD : settlement;
        List<CoaSlot> slots = new ArrayList<>();
        // Settlement-currency books
        slots.add(new CoaSlot(settle, AccountRole.AVAILABLE, "0000", "Available " + settle.getIsoCode()));
        slots.add(new CoaSlot(settle, AccountRole.HELD, "0001", "Held " + settle.getIsoCode()));
        slots.add(new CoaSlot(settle, AccountRole.ADJUST, "0002", "Adjust " + settle.getIsoCode()));
        if (settle == Currency.LP) {
            // LP-only wallet: add loyalty lifecycle books (no second HELD/ADJUST)
            slots.add(new CoaSlot(Currency.LP, AccountRole.REDEEMED, "0012", "Redeemed LP"));
            slots.add(new CoaSlot(Currency.LP, AccountRole.EXPIRED, "0013", "Expired LP"));
        } else {
            // Standard: settlement (e.g. HKD) + full LP role set
            slots.add(new CoaSlot(Currency.LP, AccountRole.AVAILABLE, "0010", "Available LP"));
            slots.add(new CoaSlot(Currency.LP, AccountRole.HELD, "0011", "Held LP"));
            slots.add(new CoaSlot(Currency.LP, AccountRole.REDEEMED, "0012", "Redeemed LP"));
            slots.add(new CoaSlot(Currency.LP, AccountRole.EXPIRED, "0013", "Expired LP"));
            slots.add(new CoaSlot(Currency.LP, AccountRole.ADJUST, "0014", "Adjust LP"));
        }
        return slots;
    }

    /**
     * Open all template accounts under a new main COA (before wallet / set ids exist).
     */
    @Transactional
    public InitializedCoa openAccounts(Currency settlement) {
        String mainAccount = commonService.getNextMainAccount();
        CoaType coaType = CoaType.LIABILITY;
        List<Account> opened = new ArrayList<>();
        Map<String, Account> byKey = new LinkedHashMap<>();
        Account primary = null;

        for (CoaSlot slot : defaultSlots(settlement)) {
            String fullNumber = CoaCodes.fullNumber(mainAccount, slot.subAccount(), coaType, slot.currency());
            Account account = accountRepository.save(Account.builder()
                .fullNumber(fullNumber)
                .entity(CoaCodes.ENTITY)
                .type(CoaCodes.typeCode(coaType))
                .subType(CoaCodes.SUB_TYPE)
                .mainAccount(mainAccount)
                .subAccount(slot.subAccount())
                .buffer(CoaCodes.BUFFER)
                .currency(slot.currency())
                .accountRole(slot.role())
                .displayName(slot.displayName())
                .status(AccountStatus.ACTIVE)
                .allowNegative(false)
                .build());
            opened.add(account);
            byKey.put(key(slot.currency(), slot.role()), account);
            if (slot.role() == AccountRole.AVAILABLE && slot.currency() == settlement) {
                primary = account;
            }
        }
        if (primary == null) {
            primary = opened.get(0);
        }
        return new InitializedCoa(mainAccount, primary, opened, byKey);
    }

    /**
     * Create DEFAULT AccountSet for wallet and attach accounts.
     */
    @Transactional
    public AccountSet attachDefaultSet(Long walletId, List<Account> accounts) {
        AccountSet set = new AccountSet();
        set.setWalletId(walletId);
        set.setCode(AccountSet.CODE_DEFAULT);
        set.setName("Default");
        set.setStatus(com.altech.ledger.entity.enu.AccountSetStatus.ACTIVE);
        set = accountSetRepository.save(set);
        for (Account a : accounts) {
            a.setAccountSetId(set.getId());
            accountRepository.save(a);
        }
        return set;
    }

    /**
     * Ensure AVAILABLE book exists for currency under wallet main (earn path / auto-create).
     */
    @Transactional
    public Account ensureAvailable(String mainAccount, Currency currency, Long accountSetId) {
        return accountRepository
            .findFirstByMainAccountAndCurrencyAndAccountRole(mainAccount, currency, AccountRole.AVAILABLE)
            .or(() -> accountSetId == null ? java.util.Optional.empty()
                : accountRepository.findByAccountSetIdAndCurrencyAndAccountRole(
                    accountSetId, currency, AccountRole.AVAILABLE))
            .orElseGet(() -> {
                // allocate free sub
                int n = 20;
                String sub;
                do {
                    sub = CoaCodes.subAccountCode(null, n++);
                } while (accountRepository.findByMainAccountAndSubAccount(mainAccount, sub).isPresent() && n < 200);
                CoaType coaType = CoaType.LIABILITY;
                Account a = accountRepository.save(Account.builder()
                    .fullNumber(CoaCodes.fullNumber(mainAccount, sub, coaType, currency))
                    .entity(CoaCodes.ENTITY)
                    .type(CoaCodes.typeCode(coaType))
                    .subType(CoaCodes.SUB_TYPE)
                    .mainAccount(mainAccount)
                    .subAccount(sub)
                    .buffer(CoaCodes.BUFFER)
                    .currency(currency)
                    .accountSetId(accountSetId)
                    .accountRole(AccountRole.AVAILABLE)
                    .displayName("Available " + currency.getIsoCode())
                    .status(AccountStatus.ACTIVE)
                    .allowNegative(false)
                    .build());
                return a;
            });
    }

    public static String key(Currency ccy, AccountRole role) {
        return ccy.getIsoCode() + ":" + role.name();
    }
}
