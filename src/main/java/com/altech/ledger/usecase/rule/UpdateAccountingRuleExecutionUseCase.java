package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.UpdateAccountingRuleExecutionRequestDto;
import com.altech.ledger.entity.dto.response.GetAccountingRuleExecutionResponseDto;
import com.altech.ledger.entity.po.accounting.AccountingRuleExecution;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.AccountingRuleErrorResponse;
import com.altech.ledger.repository.AccountingRuleExecutionRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateAccountingRuleExecutionUseCase {
    private final AccountingRuleExecutionRepository accountingRuleExecutionRepository;
    private final AccountingRuleExecutionWriter writer;

    @Transactional
    public GetAccountingRuleExecutionResponseDto execute(Long id, UpdateAccountingRuleExecutionRequestDto dto) {
        AccountingRuleExecution re = accountingRuleExecutionRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "AccountingRuleExecution not found: " + id));
        if (dto.name() != null && !dto.name().isBlank() && !dto.name().equals(re.getName())) {
            if (accountingRuleExecutionRepository.findByName(dto.name().trim()).isPresent()) {
                throw new BizException(AccountingRuleErrorResponse.RUL0409, "Name exists: " + dto.name());
            }
            re.setName(dto.name().trim());
        }
        if (dto.description() != null) {
            re.setDescription(dto.description());
        }
        if (dto.orderType() != null) {
            re.setOrderType(dto.orderType());
        }
        if (dto.eventType() != null) {
            writer.bindEventType(re, dto.eventType());
        }
        String meta = writer.encodeMetadata(dto.rules(), dto.metadata());
        if (meta != null) {
            re.setMetadata(meta);
        }
        return DtoMapper.toAccountingRuleExecution(accountingRuleExecutionRepository.save(re));
    }
}
