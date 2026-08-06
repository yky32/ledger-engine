package com.altech.ledger.usecase.account;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.accounting.RuleExecution;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.RuleExecutionRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleExecutionUseCase {
    private final RuleExecutionRepository executions;

    public RuleExecutionUseCase(RuleExecutionRepository executions) {
        this.executions = executions;
    }

    @Transactional
    public RuleExecutionResponse create(CreateRuleExecutionRequest dto) {
        if (executions.findByName(dto.name()).isPresent()) {
            throw LedgerException.conflict("RULE_EXECUTION_EXISTS", "Name exists: " + dto.name());
        }
        RuleExecution re = new RuleExecution(dto.name(), dto.description(), dto.orderType(), dto.metadata());
        return DtoMapper.toRuleExecution(executions.save(re));
    }

    @Transactional(readOnly = true)
    public RuleExecutionResponse getOne(Long id) {
        return DtoMapper.toRuleExecution(executions.findById(id)
            .orElseThrow(() -> LedgerException.notFound("RuleExecution not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<RuleExecutionResponse> getAll(Pageable pageable) {
        return executions.findAll(pageable).map(DtoMapper::toRuleExecution);
    }

    @Transactional(readOnly = true)
    public RuleExecutionResponse fetchByOrderType(OrderType orderType) {
        return findByOrderType(orderType)
            .orElseThrow(() -> LedgerException.notFound("No rule execution for " + orderType));
    }

    /** Soft lookup — does not throw (for execution path). */
    @Transactional(readOnly = true)
    public java.util.Optional<RuleExecutionResponse> findByOrderType(OrderType orderType) {
        return executions.findAll().stream()
            .filter(r -> r.getOrderType() == orderType)
            .findFirst()
            .map(DtoMapper::toRuleExecution);
    }
}
