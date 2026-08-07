package com.altech.ledger.endpoint.accounting;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.usecase.rule.CreateRuleUseCase;
import com.altech.ledger.usecase.rule.QueryRuleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleEndpoint {
    private final CreateRuleUseCase createRuleUseCase;
    private final QueryRuleUseCase queryRuleUseCase;

    @PostMapping
    public Result<RuleDtos.Response> create(@Valid @RequestBody RuleDtos.CreateRequest dto) {
        return R.success(createRuleUseCase.execute(dto));
    }

    @GetMapping("/{id}")
    public Result<RuleDtos.Response> getOne(@PathVariable Long id) {
        return R.success(queryRuleUseCase.one(id));
    }

    @GetMapping
    public Result<List<RuleDtos.Response>> getAll(@PageableDefault(size = 50) Pageable pageable) {
        var page = queryRuleUseCase.list(pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
