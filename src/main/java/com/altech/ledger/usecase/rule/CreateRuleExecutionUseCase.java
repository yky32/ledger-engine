package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.entity.po.accounting.RuleExecution;
import com.altech.ledger.exception.response.RuleErrorResponse;
import com.altech.ledger.repository.RuleExecutionRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateRuleExecutionUseCase {
    private final RuleExecutionRepository ruleExecutionRepository;

    @Transactional
    public RuleDtos.ExecutionResponse execute(RuleDtos.CreateExecutionRequest dto) {
        if (ruleExecutionRepository.findByName(dto.name()).isPresent()) {
            throw new BizException(RuleErrorResponse.RUL0409, "Name exists: " + dto.name());
        }
        RuleExecution re = new RuleExecution(dto.name(), dto.description(), dto.orderType(), dto.metadata());
        return DtoMapper.toRuleExecution(ruleExecutionRepository.save(re));
    }
}
