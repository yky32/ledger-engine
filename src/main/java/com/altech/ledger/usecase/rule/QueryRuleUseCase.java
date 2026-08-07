package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.RuleRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class QueryRuleUseCase {
    private final RuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public RuleDtos.Response one(Long id) {
        return DtoMapper.toRule(ruleRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Rule not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<RuleDtos.Response> list(Pageable pageable) {
        return ruleRepository.findAll(pageable).map(DtoMapper::toRule);
    }
}
