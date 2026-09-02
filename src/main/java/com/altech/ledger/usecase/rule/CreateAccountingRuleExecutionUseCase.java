package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateAccountingRuleExecutionRequestDto;
import com.altech.ledger.entity.dto.response.GetAccountingRuleExecutionResponseDto;
import com.altech.ledger.entity.po.accounting.AccountingRuleExecution;
import com.altech.ledger.exception.response.AccountingRuleErrorResponse;
import com.altech.ledger.repository.AccountingRuleExecutionRepository;
import com.altech.ledger.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateAccountingRuleExecutionUseCase {
    private final AccountingRuleExecutionRepository accountingRuleExecutionRepository;
    private final AccountingRuleExecutionWriter writer;

    @Transactional
    public GetAccountingRuleExecutionResponseDto execute(CreateAccountingRuleExecutionRequestDto dto) {
        if (accountingRuleExecutionRepository.findByName(dto.name()).isPresent()) {
            throw new BizException(AccountingRuleErrorResponse.RUL0409, "Name exists: " + dto.name());
        }
        AccountingRuleExecution re = new AccountingRuleExecution();
        re.setName(dto.name().trim());
        re.setDescription(dto.description());
        re.setOrderType(dto.orderType());
        re.setIsActive(true);
        re.setMetadata(writer.encodeMetadata(dto.rules(), dto.metadata()));
        accountingRuleExecutionRepository.saveAndFlush(re);
        writer.bindEventType(re, dto.eventType());
        return DtoWrapper.getAccountingRuleExecutionResponseDto(accountingRuleExecutionRepository.save(re));
    }
}
