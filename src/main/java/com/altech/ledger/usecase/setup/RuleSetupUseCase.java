package com.altech.ledger.usecase.setup;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.po.accounting.Rule;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.RuleRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleSetupUseCase {
    private final RuleRepository rules;

    public RuleSetupUseCase(RuleRepository rules) {
        this.rules = rules;
    }

    @Transactional
    public RuleResponse create(CreateRuleRequest dto) {
        Rule rule = new Rule(dto.name(), dto.description(), dto.direction(),
            dto.multiplier(), dto.targetAccount(), dto.content());
        return DtoMapper.toRule(rules.save(rule));
    }

    @Transactional(readOnly = true)
    public RuleResponse getOne(Long id) {
        return DtoMapper.toRule(rules.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Rule not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<RuleResponse> getAll(Pageable pageable) {
        return rules.findAll(pageable).map(DtoMapper::toRule);
    }
}
