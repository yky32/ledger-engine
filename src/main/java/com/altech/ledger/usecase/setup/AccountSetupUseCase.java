package com.altech.ledger.usecase.setup;

import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.service.CommonService;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.account.LedgerAccountDtos;

@Service
@RequiredArgsConstructor
public class AccountSetupUseCase {
    private final AccountRepository accounts;
    private final CommonService commonService;

    @Transactional
    public LedgerAccountDtos.Response create(LedgerAccountDtos.CreateRequest dto) {
        String currency = dto.currency() == null ? null : dto.currency().toUpperCase();
        if (currency == null || !currency.matches("[A-Z]{2,4}")) {
            throw LedgerException.badRequest("INVALID_CURRENCY", "Currency must be 2-4 uppercase letters");
        }
        String entity = blank(dto.entity(), "10");
        String type = blank(dto.type(), "99");
        String subType = blank(dto.subType(), "00");
        String buffer = blank(dto.buffer(), "NA");
        String mainAccount = blank(dto.mainAccount(), commonService.getNextMainAccount());
        String subAccount = blank(dto.subAccount(), "0000");
        String fullNumber = entity + type + subType + mainAccount + subAccount + buffer + currency;

        if (accounts.existsByFullNumber(fullNumber)) {
            throw LedgerException.conflict("ACCOUNT_EXISTS", "Account already exists: " + fullNumber);
        }
        if (accounts.findByMainAccountAndSubAccount(mainAccount, subAccount).isPresent()) {
            throw LedgerException.conflict("ACCOUNT_EXISTS",
                "Main/sub account already exists: " + mainAccount + "/" + subAccount);
        }

        boolean allowNegative = dto.allowNegative() != null && dto.allowNegative();
        Account account = new Account(fullNumber, entity, type, subType, mainAccount, subAccount,
            buffer, currency, allowNegative);
        return DtoMapper.toAccount(accounts.save(account));
    }

    @Transactional
    public List<LedgerAccountDtos.Response> createByAssociatedCurrencies(String mainAccount, List<String> currencies) {
        List<LedgerAccountDtos.Response> created = new ArrayList<>();
        for (String currency : currencies) {
            LedgerAccountDtos.CreateRequest req = new LedgerAccountDtos.CreateRequest(
                "10", "99", "00", "NA", mainAccount, commonService.getNextSubAccount(mainAccount),
                currency.toUpperCase(), false);
            created.add(create(req));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public LedgerAccountDtos.Response getOne(Long id) {
        return DtoMapper.toAccount(accounts.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Account not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<LedgerAccountDtos.Response> getAll(Pageable pageable) {
        return accounts.findAll(pageable).map(DtoMapper::toAccount);
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
