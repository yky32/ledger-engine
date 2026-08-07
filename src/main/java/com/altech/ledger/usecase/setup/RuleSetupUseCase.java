package com.altech.ledger.usecase.setup;

import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.entity.po.accounting.Rule;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.RuleRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleSetupUseCase {
    private final RuleRepository rules;

    @Transactional
    public RuleDtos.Response create(RuleDtos.CreateRequest dto) {
        Rule rule = new Rule(dto.name(), dto.description(), dto.direction(),
            dto.multiplier(), dto.targetAccount(), dto.content());
        return DtoMapper.toRule(rules.save(rule));
    }

    @Transactional(readOnly = true)
    public RuleDtos.Response getOne(Long id) {
        return DtoMapper.toRule(rules.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Rule not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<RuleDtos.Response> getAll(Pageable pageable) {
        return rules.findAll(pageable).map(DtoMapper::toRule);
    }
}
