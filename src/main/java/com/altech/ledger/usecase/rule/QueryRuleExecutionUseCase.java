package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.RuleExecutionRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QueryRuleExecutionUseCase {
    private final RuleExecutionRepository executions;

    @Transactional(readOnly = true)
    public RuleDtos.ExecutionResponse one(Long id) {
        return DtoMapper.toRuleExecution(executions.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "RuleExecution not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<RuleDtos.ExecutionResponse> list(Pageable pageable) {
        return executions.findAll(pageable).map(DtoMapper::toRuleExecution);
    }

    @Transactional(readOnly = true)
    public RuleDtos.ExecutionResponse byOrderType(OrderType orderType) {
        return findByOrderType(orderType)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "No rule execution for " + orderType));
    }

    /** Soft lookup — does not throw (for execution path). */
    @Transactional(readOnly = true)
    public Optional<RuleDtos.ExecutionResponse> findByOrderType(OrderType orderType) {
        return executions.findAll().stream()
            .filter(r -> r.getOrderType() == orderType)
            .findFirst()
            .map(DtoMapper::toRuleExecution);
    }
}
