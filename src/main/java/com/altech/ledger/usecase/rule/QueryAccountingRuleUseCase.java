package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.AccountingRuleRepository;
import com.altech.ledger.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.altech.ledger.util.Pageables;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.response.GetAccountingRuleResponseDto;

@Component
@RequiredArgsConstructor
public class QueryAccountingRuleUseCase {
    private final AccountingRuleRepository accountingRuleRepository;

    @Transactional(readOnly = true)
    public GetAccountingRuleResponseDto one(Long id) {
        return DtoWrapper.getAccountingRuleResponseDto(accountingRuleRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "AccountingRule not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<GetAccountingRuleResponseDto> list(Pageable pageable) {
        return accountingRuleRepository.findAll(Pageables.toZeroBased(pageable)).map(DtoWrapper::getAccountingRuleResponseDto);
    }
}
