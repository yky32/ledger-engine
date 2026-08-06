package com.altech.ledger.endpoint.accounting;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.usecase.setup.RuleSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleEndpoint {
    private final RuleSetupUseCase useCase;

    @PostMapping
    public RuleResponse create(@Valid @RequestBody CreateRuleRequest dto) {
        return useCase.create(dto);
    }

    @GetMapping("/{id}")
    public RuleResponse getOne(@PathVariable Long id) {
        return useCase.getOne(id);
    }

    @GetMapping
    public Page<RuleResponse> getAll(@PageableDefault(size = 50) Pageable pageable) {
        return useCase.getAll(pageable);
    }
}
