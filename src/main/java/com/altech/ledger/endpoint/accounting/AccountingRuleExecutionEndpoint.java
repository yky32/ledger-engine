package com.altech.ledger.endpoint.accounting;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.rule.CreateAccountingRuleExecutionUseCase;
import com.altech.ledger.usecase.rule.QueryAccountingRuleExecutionUseCase;
import com.altech.ledger.usecase.rule.UpdateAccountingRuleExecutionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.altech.ledger.entity.dto.request.CreateAccountingRuleExecutionRequestDto;
import com.altech.ledger.entity.dto.request.UpdateAccountingRuleExecutionRequestDto;
import com.altech.ledger.entity.dto.response.GetAccountingRuleExecutionResponseDto;

/**
 * Ordered posting sequence of {@code AccountingRule} legs, keyed by eventType / orderType.
 * Not COA (chart) and not Digestion Brain.
 */
@RestController
@RequestMapping("/accounting-rule-executions")
@RequiredArgsConstructor
public class AccountingRuleExecutionEndpoint {
    private final CreateAccountingRuleExecutionUseCase createAccountingRuleExecutionUseCase;
    private final UpdateAccountingRuleExecutionUseCase updateAccountingRuleExecutionUseCase;
    private final QueryAccountingRuleExecutionUseCase queryAccountingRuleExecutionUseCase;

    @PostMapping
    public Result<GetAccountingRuleExecutionResponseDto> create(@Valid @RequestBody CreateAccountingRuleExecutionRequestDto dto) {
        return R.success(createAccountingRuleExecutionUseCase.execute(dto));
    }

    @PutMapping("/{id}")
    public Result<GetAccountingRuleExecutionResponseDto> update(
        @PathVariable Long id,
        @RequestBody UpdateAccountingRuleExecutionRequestDto dto
    ) {
        return R.success(updateAccountingRuleExecutionUseCase.execute(id, dto));
    }

    @GetMapping("/{id}")
    public Result<GetAccountingRuleExecutionResponseDto> getOne(@PathVariable Long id) {
        return R.success(queryAccountingRuleExecutionUseCase.one(id));
    }

    @GetMapping
    public Result<List<GetAccountingRuleExecutionResponseDto>> getAll(@PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC)
        Pageable pageable) {
        var page = queryAccountingRuleExecutionUseCase.list(pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
