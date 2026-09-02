package com.altech.ledger.usecase.ledger;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.service.CommonService;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product ledger account create — numeric COA fullNumber only.
 */
@Component
@RequiredArgsConstructor
public class CreateLedgerAccountUseCase {
    private final AccountRepository accountRepository;
    private final CommonService commonService;
    private final CommonUseCase commonUseCase;

    @Transactional
    public AccountResponse execute(CreateAccountRequest request) {
        commonUseCase.requireCurrency(request.currency());
        String mainAccount = commonService.getNextMainAccount();
        String fullNumber = CoaCodes.fullNumber(mainAccount, request.type(), request.currency());

        if (accountRepository.existsByFullNumber(fullNumber)) {
            throw new BizException(AccountErrorResponse.ACC0409, "Account already exists: " + fullNumber);
        }

        Account account = Account.builder()
            .fullNumber(fullNumber)
            .entity(CoaCodes.ENTITY)
            .type(CoaCodes.typeCode(request.type()))
            .subType(CoaCodes.SUB_TYPE)
            .mainAccount(mainAccount)
            .buffer(CoaCodes.BUFFER)
            .currency(request.currency())
            .allowNegative(request.allowNegative())
            .build();
        return _toResponse(accountRepository.save(account));
    }

    private AccountResponse _toResponse(Account a) {
        CoaType coa;
        try {
            // type stored as numeric code; reverse map common ones
            coa = switch (a.getType() == null ? "" : a.getType()) {
                case "10" -> CoaType.ASSET;
                case "20" -> CoaType.LIABILITY;
                case "30" -> CoaType.EQUITY;
                case "40" -> CoaType.REVENUE;
                case "50" -> CoaType.EXPENSE;
                default -> CoaType.valueOf(a.getType());
            };
        } catch (Exception ex) {
            coa = CoaType.LIABILITY;
        }
        return new AccountResponse(
            a.getId(), a.getFullNumber(),
            a.getCurrency() != null ? a.getCurrency().getIsoCode() : a.getFullNumber(),
            coa, a.getCurrency(), a.getStatus(),
            a.isAllowNegative(), a.getLedgerBalance(), a.getAvailableBalance(), a.getVersion(),
            a.getCreateDt(), a.getUpdateDt());
    }
}
