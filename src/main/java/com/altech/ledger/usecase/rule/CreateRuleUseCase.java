package com.altech.ledger.usecase.rule;

import com.altech.ledger.entity.po.accounting.Rule;
import com.altech.ledger.repository.RuleRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.request.CreateRuleRequestDto;
import com.altech.ledger.entity.dto.response.GetRuleResponseDto;

@Component
@RequiredArgsConstructor
public class CreateRuleUseCase {
    private final RuleRepository ruleRepository;

    @Transactional
    public GetRuleResponseDto execute(CreateRuleRequestDto dto) {
        Rule rule = new Rule(dto.name(), dto.description(), dto.direction(),
            dto.multiplier(), dto.targetAccount(), dto.content());
        return DtoMapper.toRule(ruleRepository.save(rule));
    }
}
