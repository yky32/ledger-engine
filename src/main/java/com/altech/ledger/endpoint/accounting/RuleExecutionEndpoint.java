package com.altech.ledger.endpoint.accounting;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.usecase.rule.CreateRuleExecutionUseCase;
import com.altech.ledger.usecase.rule.QueryRuleExecutionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rule-executions")
@RequiredArgsConstructor
public class RuleExecutionEndpoint {
    private final CreateRuleExecutionUseCase createRuleExecutionUseCase;
    private final QueryRuleExecutionUseCase queryRuleExecutionUseCase;

    @PostMapping
    public Result<RuleDtos.ExecutionResponse> create(@Valid @RequestBody RuleDtos.CreateExecutionRequest dto) {
        return R.success(createRuleExecutionUseCase.execute(dto));
    }

    @GetMapping("/{id}")
    public Result<RuleDtos.ExecutionResponse> getOne(@PathVariable Long id) {
        return R.success(queryRuleExecutionUseCase.one(id));
    }

    @GetMapping
    public Result<List<RuleDtos.ExecutionResponse>> getAll(@PageableDefault(size = 50) Pageable pageable) {
        var page = queryRuleExecutionUseCase.list(pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
