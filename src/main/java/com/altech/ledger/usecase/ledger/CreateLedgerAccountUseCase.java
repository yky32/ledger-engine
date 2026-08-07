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
        String subAccount = CoaCodes.PRIMARY_SUB;
        String fullNumber = CoaCodes.fullNumber(mainAccount, subAccount, request.type(), request.currency());

        if (accountRepository.existsByFullNumber(fullNumber)) {
            throw new BizException(AccountErrorResponse.ACC0409, "Account already exists: " + fullNumber);
        }
        if (accountRepository.findByMainAccountAndSubAccount(mainAccount, subAccount).isPresent()) {
            throw new BizException(AccountErrorResponse.ACC0409,
                "Main/sub account already exists: " + mainAccount + "/" + subAccount);
        }

        Account account = new Account(
            fullNumber,
            CoaCodes.ENTITY,
            CoaCodes.typeCode(request.type()),
            CoaCodes.SUB_TYPE,
            mainAccount,
            subAccount,
            CoaCodes.BUFFER,
            request.currency(),
            request.allowNegative()
        );
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
            a.getId(), a.getFullNumber(), a.getSubAccount(), coa, a.getCurrency(), a.getStatus(),
            a.isAllowNegative(), a.getLedgerBalance(), a.getAvailableBalance(), a.getVersion(),
            a.getCreateDt(), a.getUpdateDt());
    }
}
