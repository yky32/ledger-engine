package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.RuleExecutionRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.altech.ledger.util.Pageables;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import com.altech.ledger.entity.dto.response.GetRuleExecutionResponseDto;

@Component
@RequiredArgsConstructor
public class QueryRuleExecutionUseCase {
    private final RuleExecutionRepository ruleExecutionRepository;

    @Transactional(readOnly = true)
    public GetRuleExecutionResponseDto one(Long id) {
        return DtoMapper.toRuleExecution(ruleExecutionRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "RuleExecution not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<GetRuleExecutionResponseDto> list(Pageable pageable) {
        return ruleExecutionRepository.findAll(Pageables.toZeroBased(pageable)).map(DtoMapper::toRuleExecution);
    }

    @Transactional(readOnly = true)
    public GetRuleExecutionResponseDto byOrderType(OrderType orderType) {
        return findByOrderType(orderType)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "No rule execution for " + orderType));
    }

    /** Soft lookup — does not throw (for execution path). */
    @Transactional(readOnly = true)
    public Optional<GetRuleExecutionResponseDto> findByOrderType(OrderType orderType) {
        return ruleExecutionRepository.findAll().stream()
            .filter(r -> r.getOrderType() == orderType)
            .findFirst()
            .map(DtoMapper::toRuleExecution);
    }
}
