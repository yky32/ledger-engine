package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.UpdateAccountingRuleRequestDto;
import com.altech.ledger.entity.dto.response.GetAccountingRuleResponseDto;
import com.altech.ledger.entity.po.accounting.AccountingRule;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.AccountingRuleRepository;
import com.altech.ledger.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateAccountingRuleUseCase {
    private final AccountingRuleRepository accountingRuleRepository;

    @Transactional
    public GetAccountingRuleResponseDto execute(Long id, UpdateAccountingRuleRequestDto dto) {
        AccountingRule rule = accountingRuleRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "AccountingRule not found: " + id));
        if (dto.name() != null && !dto.name().isBlank()) {
            rule.setName(dto.name().trim());
        }
        if (dto.description() != null) {
            rule.setDescription(dto.description());
        }
        if (dto.direction() != null) {
            rule.setDirection(dto.direction());
        }
        if (dto.multiplier() != null) {
            rule.setMultiplier(dto.multiplier());
        }
        if (dto.targetAccount() != null && !dto.targetAccount().isBlank()) {
            rule.setTargetAccount(dto.targetAccount().trim().toUpperCase());
        }
        if (dto.content() != null) {
            rule.setContent(dto.content());
        }
        return DtoWrapper.getAccountingRuleResponseDto(accountingRuleRepository.save(rule));
    }
}
