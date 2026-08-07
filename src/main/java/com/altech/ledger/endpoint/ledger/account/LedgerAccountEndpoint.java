package com.altech.ledger.endpoint.ledger.account;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.parity.LedgerAccountDtos;
import com.altech.ledger.usecase.account.CreateAccountUseCase;
import com.altech.ledger.usecase.account.QueryAccountUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ledger-accounts")
@RequiredArgsConstructor
public class LedgerAccountEndpoint {
    private final CreateAccountUseCase createAccountUseCase;
    private final QueryAccountUseCase queryAccountUseCase;

    @PostMapping
    public Result<LedgerAccountDtos.Response> create(@Valid @RequestBody LedgerAccountDtos.CreateRequest dto) {
        return R.success(createAccountUseCase.execute(dto));
    }

    @GetMapping("/{id}")
    public Result<LedgerAccountDtos.Response> getOne(@PathVariable Long id) {
        return R.success(queryAccountUseCase.one(id));
    }

    @GetMapping
    public Result<List<LedgerAccountDtos.Response>> getAll(@PageableDefault(size = 50) Pageable pageable) {
        var page = queryAccountUseCase.list(pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
