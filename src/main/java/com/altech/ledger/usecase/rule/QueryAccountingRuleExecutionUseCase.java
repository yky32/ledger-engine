package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.AccountingRuleExecutionRepository;
import com.altech.ledger.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.altech.ledger.util.Pageables;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import com.altech.ledger.entity.dto.response.GetAccountingRuleExecutionResponseDto;

@Component
@RequiredArgsConstructor
public class QueryAccountingRuleExecutionUseCase {
    private final AccountingRuleExecutionRepository accountingRuleExecutionRepository;

    @Transactional(readOnly = true)
    public GetAccountingRuleExecutionResponseDto one(Long id) {
        return DtoWrapper.getAccountingRuleExecutionResponseDto(accountingRuleExecutionRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "AccountingRuleExecution not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<GetAccountingRuleExecutionResponseDto> list(Pageable pageable) {
        return accountingRuleExecutionRepository.findAll(Pageables.toZeroBased(pageable))
            .map(DtoWrapper::getAccountingRuleExecutionResponseDto);
    }

    @Transactional(readOnly = true)
    public GetAccountingRuleExecutionResponseDto byOrderType(OrderType orderType) {
        return findByOrderType(orderType)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "No accounting rule execution for " + orderType));
    }

    /** Soft lookup — does not throw (for execution path). */
    @Transactional(readOnly = true)
    public Optional<GetAccountingRuleExecutionResponseDto> findByOrderType(OrderType orderType) {
        return accountingRuleExecutionRepository.findAll().stream()
            .filter(r -> r.getOrderType() == orderType)
            .findFirst()
            .map(DtoWrapper::getAccountingRuleExecutionResponseDto);
    }
}
