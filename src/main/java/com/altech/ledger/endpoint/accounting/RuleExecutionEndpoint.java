package com.altech.ledger.endpoint.accounting;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.rule.CreateRuleExecutionUseCase;
import com.altech.ledger.usecase.rule.QueryRuleExecutionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.altech.ledger.entity.dto.request.CreateRuleExecutionRequestDto;
import com.altech.ledger.entity.dto.response.GetRuleExecutionResponseDto;

@RestController
@RequestMapping("/rule-executions")
@RequiredArgsConstructor
public class RuleExecutionEndpoint {
    private final CreateRuleExecutionUseCase createRuleExecutionUseCase;
    private final QueryRuleExecutionUseCase queryRuleExecutionUseCase;

    @PostMapping
    public Result<GetRuleExecutionResponseDto> create(@Valid @RequestBody CreateRuleExecutionRequestDto dto) {
        return R.success(createRuleExecutionUseCase.execute(dto));
    }

    @GetMapping("/{id}")
    public Result<GetRuleExecutionResponseDto> getOne(@PathVariable Long id) {
        return R.success(queryRuleExecutionUseCase.one(id));
    }

    @GetMapping
    public Result<List<GetRuleExecutionResponseDto>> getAll(@PageableDefault(size = 50) Pageable pageable) {
        var page = queryRuleExecutionUseCase.list(pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
