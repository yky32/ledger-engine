package com.altech.ledger.endpoint.accounting;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.rule.CreateRuleUseCase;
import com.altech.ledger.usecase.rule.QueryRuleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.altech.ledger.entity.dto.request.CreateRuleRequestDto;
import com.altech.ledger.entity.dto.response.GetRuleResponseDto;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleEndpoint {
    private final CreateRuleUseCase createRuleUseCase;
    private final QueryRuleUseCase queryRuleUseCase;

    @PostMapping
    public Result<GetRuleResponseDto> create(@Valid @RequestBody CreateRuleRequestDto dto) {
        return R.success(createRuleUseCase.execute(dto));
    }

    @GetMapping("/{id}")
    public Result<GetRuleResponseDto> getOne(@PathVariable Long id) {
        return R.success(queryRuleUseCase.one(id));
    }

    @GetMapping
    public Result<List<GetRuleResponseDto>> getAll(@PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC)
        Pageable pageable) {
        var page = queryRuleUseCase.list(pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
