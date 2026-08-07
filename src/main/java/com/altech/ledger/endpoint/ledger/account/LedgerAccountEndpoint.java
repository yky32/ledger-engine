package com.altech.ledger.endpoint.ledger.account;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.account.CreateAccountUseCase;
import com.altech.ledger.usecase.account.QueryAccountUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.altech.ledger.entity.dto.request.CreateLedgerAccountRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;

@RestController
@RequestMapping("/ledger-accounts")
@RequiredArgsConstructor
public class LedgerAccountEndpoint {
    private final CreateAccountUseCase createAccountUseCase;
    private final QueryAccountUseCase queryAccountUseCase;

    @PostMapping
    public Result<GetLedgerAccountResponseDto> create(@Valid @RequestBody CreateLedgerAccountRequestDto dto) {
        return R.success(createAccountUseCase.execute(dto));
    }

    @GetMapping("/{id}")
    public Result<GetLedgerAccountResponseDto> getOne(@PathVariable Long id) {
        return R.success(queryAccountUseCase.one(id));
    }

    @GetMapping
    public Result<List<GetLedgerAccountResponseDto>> getAll(@PageableDefault(size = 50) Pageable pageable) {
        var page = queryAccountUseCase.list(pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
