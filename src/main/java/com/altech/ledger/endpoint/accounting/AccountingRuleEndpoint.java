package com.altech.ledger.endpoint.accounting;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.rule.AccountingRuleCatalogUseCase;
import com.altech.ledger.usecase.rule.CreateAccountingRuleUseCase;
import com.altech.ledger.usecase.rule.QueryAccountingRuleUseCase;
import com.altech.ledger.usecase.rule.UpdateAccountingRuleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.altech.ledger.entity.dto.request.CreateAccountingRuleRequestDto;
import com.altech.ledger.entity.dto.request.UpdateAccountingRuleRequestDto;
import com.altech.ledger.entity.dto.response.GetAccountingRuleResponseDto;
import com.altech.ledger.entity.dto.response.GetAccountingRulesBundleDto;

/**
 * One posting-sequence leg: direction, multiplier, {@code targetAccount} = CoaProfile.code.
 * Not COA (chart) and not Digestion Brain.
 */
@RestController
@RequestMapping("/accounting-rules")
@RequiredArgsConstructor
public class AccountingRuleEndpoint {
    private final CreateAccountingRuleUseCase createAccountingRuleUseCase;
    private final UpdateAccountingRuleUseCase updateAccountingRuleUseCase;
    private final QueryAccountingRuleUseCase queryAccountingRuleUseCase;
    private final AccountingRuleCatalogUseCase accountingRuleCatalogUseCase;

    /** createIfNotFound UA Transaction → HKD / LP sequences + member/house COA. */
    @PostMapping("/ensure")
    public Result<GetAccountingRulesBundleDto> ensure() {
        return R.success(accountingRuleCatalogUseCase.ensureAndList());
    }

    @PostMapping
    public Result<GetAccountingRuleResponseDto> create(@Valid @RequestBody CreateAccountingRuleRequestDto dto) {
        return R.success(createAccountingRuleUseCase.execute(dto));
    }

    @PutMapping("/{id}")
    public Result<GetAccountingRuleResponseDto> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAccountingRuleRequestDto dto
    ) {
        return R.success(updateAccountingRuleUseCase.execute(id, dto));
    }

    @GetMapping("/{id}")
    public Result<GetAccountingRuleResponseDto> getOne(@PathVariable Long id) {
        return R.success(queryAccountingRuleUseCase.one(id));
    }

    @GetMapping
    public Result<List<GetAccountingRuleResponseDto>> getAll(@PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC)
        Pageable pageable) {
        var page = queryAccountingRuleUseCase.list(pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
