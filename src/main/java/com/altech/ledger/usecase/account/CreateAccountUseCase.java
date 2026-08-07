package com.altech.ledger.usecase.account;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateLedgerAccountRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.service.CommonService;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CreateAccountUseCase {
    private final AccountRepository accountRepository;
    private final CommonService commonService;
    private final CommonUseCase commonUseCase;

    @Transactional
    public GetLedgerAccountResponseDto execute(CreateLedgerAccountRequestDto dto) {
        Currency currency = commonUseCase.requireCurrency(dto.currency());
        String entity = _blank(dto.entity(), "10");
        String type = _blank(dto.type(), "99");
        String subType = _blank(dto.subType(), "00");
        String buffer = _blank(dto.buffer(), "NA");
        String mainAccount = _blank(dto.mainAccount(), commonService.getNextMainAccount());
        String subAccount = _blank(dto.subAccount(), "0000");
        String fullNumber = entity + type + subType + mainAccount + subAccount + buffer + currency.getIsoCode();

        if (accountRepository.existsByFullNumber(fullNumber)) {
            throw new BizException(AccountErrorResponse.ACC0409, "Account already exists: " + fullNumber);
        }
        if (accountRepository.findByMainAccountAndSubAccount(mainAccount, subAccount).isPresent()) {
            throw new BizException(AccountErrorResponse.ACC0409,
                "Main/sub account already exists: " + mainAccount + "/" + subAccount);
        }

        boolean allowNegative = dto.allowNegative() != null && dto.allowNegative();
        Account account = new Account(fullNumber, entity, type, subType, mainAccount, subAccount,
            buffer, currency, allowNegative);
        return DtoMapper.toAccount(accountRepository.save(account));
    }

    @Transactional
    public List<GetLedgerAccountResponseDto> executeByAssociatedCurrencies(String mainAccount, List<String> currencies) {
        List<GetLedgerAccountResponseDto> created = new ArrayList<>();
        for (String currencyCode : currencies) {
            Currency currency = commonUseCase.requireCurrency(currencyCode);
            CreateLedgerAccountRequestDto req = new CreateLedgerAccountRequestDto(
                "10", "99", "00", "NA", mainAccount, commonService.getNextSubAccount(mainAccount),
                currency, false);
            created.add(execute(req));
        }
        return created;
    }

    private static String _blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
