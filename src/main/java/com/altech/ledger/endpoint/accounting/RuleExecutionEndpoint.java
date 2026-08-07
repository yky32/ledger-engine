package com.altech.ledger.endpoint.accounting;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import java.util.List;

import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.usecase.account.RuleExecutionUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rule-executions")
@RequiredArgsConstructor
public class RuleExecutionEndpoint {
    private final RuleExecutionUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<RuleDtos.ExecutionResponse> create(@Valid @RequestBody RuleDtos.CreateExecutionRequest dto) {
        return R.success(useCase.create(dto));
    }

    @GetMapping("/{id}")
    public Result<RuleDtos.ExecutionResponse> getOne(@PathVariable Long id) {
        return R.success(useCase.getOne(id));
    }

    @GetMapping
    public Result<List<RuleDtos.ExecutionResponse>> getAll(@PageableDefault(size = 50) Pageable pageable) {
        return R.success(useCase.getAll(pageable));
    }
}
