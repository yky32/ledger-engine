package com.altech.ledger.usecase.rule;

import com.altech.ledger.entity.dto.request.CreateAccountingRuleRequestDto;
import com.altech.ledger.entity.dto.response.GetAccountingRuleResponseDto;
import com.altech.ledger.entity.po.accounting.AccountingRule;
import com.altech.ledger.repository.AccountingRuleRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateAccountingRuleUseCase {
    private final AccountingRuleRepository accountingRuleRepository;

    @Transactional
    public GetAccountingRuleResponseDto execute(CreateAccountingRuleRequestDto dto) {
        AccountingRule rule = new AccountingRule();
        rule.setName(dto.name());
        rule.setDescription(dto.description());
        rule.setDirection(dto.direction());
        rule.setMultiplier(dto.multiplier());
        rule.setTargetAccount(dto.targetAccount() == null ? null : dto.targetAccount().trim().toUpperCase());
        rule.setContent(dto.content());
        rule.setIsActive(true);
        return DtoMapper.toAccountingRule(accountingRuleRepository.save(rule));
    }
}
