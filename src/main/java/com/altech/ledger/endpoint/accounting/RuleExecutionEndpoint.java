package com.altech.ledger.endpoint.accounting;

import com.altech.ledger.entity.dto.rule.RuleDtos;
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
    public RuleDtos.ExecutionResponse create(@Valid @RequestBody RuleDtos.CreateExecutionRequest dto) {
        return useCase.create(dto);
    }

    @GetMapping("/{id}")
    public RuleDtos.ExecutionResponse getOne(@PathVariable Long id) {
        return useCase.getOne(id);
    }

    @GetMapping
    public Page<RuleDtos.ExecutionResponse> getAll(@PageableDefault(size = 50) Pageable pageable) {
        return useCase.getAll(pageable);
    }
}
