package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.RuleRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.altech.ledger.util.Pageables;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.response.GetRuleResponseDto;

@Component
@RequiredArgsConstructor
public class QueryRuleUseCase {
    private final RuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public GetRuleResponseDto one(Long id) {
        return DtoMapper.toRule(ruleRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Rule not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<GetRuleResponseDto> list(Pageable pageable) {
        return ruleRepository.findAll(Pageables.toZeroBased(pageable)).map(DtoMapper::toRule);
    }
}
