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
import com.altech.ledger.util.CoaCodes;
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
        String entity = _blank(dto.entity(), CoaCodes.ENTITY);
        String type = _blank(dto.type(), "20");
        String subType = _blank(dto.subType(), CoaCodes.SUB_TYPE);
        String buffer = _blank(dto.buffer(), CoaCodes.BUFFER);
        // force numeric buffer if legacy "NA"
        if (!buffer.matches("\\d+")) {
            buffer = CoaCodes.BUFFER;
        }
        String mainAccount = _blank(dto.mainAccount(), commonService.getNextMainAccount());
        if (!mainAccount.matches("\\d+")) {
            mainAccount = commonService.getNextMainAccount();
        }
        String subAccount = _blank(dto.subAccount(), CoaCodes.PRIMARY_SUB);
        if (!subAccount.matches("\\d+")) {
            subAccount = CoaCodes.PRIMARY_SUB;
        }
        if (subAccount.length() < 4 && subAccount.matches("\\d+")) {
            subAccount = String.format("%04d", Integer.parseInt(subAccount));
        }
        String fullNumber = CoaCodes.fullNumber(entity, type, subType, mainAccount, subAccount, buffer, currency);

        if (accountRepository.existsByFullNumber(fullNumber)) {
            throw new BizException(AccountErrorResponse.ACC0409, "Account already exists: " + fullNumber);
        }
        if (accountRepository.findByMainAccountAndSubAccount(mainAccount, subAccount).isPresent()) {
            throw new BizException(AccountErrorResponse.ACC0409,
                "Main/sub account already exists: " + mainAccount + "/" + subAccount);
        }

        boolean allowNegative = dto.allowNegative() != null && dto.allowNegative();
        Account account = Account.builder()
            .walletId(dto.walletId())
            .fullNumber(fullNumber)
            .entity(entity)
            .type(type)
            .subType(subType)
            .mainAccount(mainAccount)
            .subAccount(subAccount)
            .buffer(buffer)
            .currency(currency)
            .allowNegative(allowNegative)
            .build();
        return DtoMapper.toAccount(accountRepository.save(account));
    }

    @Transactional
    public List<GetLedgerAccountResponseDto> executeByAssociatedCurrencies(String mainAccount, List<String> currencies) {
        List<GetLedgerAccountResponseDto> created = new ArrayList<>();
        for (String currencyCode : currencies) {
            Currency currency = commonUseCase.requireCurrency(currencyCode);
            CreateLedgerAccountRequestDto req = new CreateLedgerAccountRequestDto(
                CoaCodes.ENTITY, "20", CoaCodes.SUB_TYPE, CoaCodes.BUFFER, mainAccount,
                commonService.getNextSubAccount(mainAccount),
                currency, false, null);
            created.add(execute(req));
        }
        return created;
    }

    private static String _blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
