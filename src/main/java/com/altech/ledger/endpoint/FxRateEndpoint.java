package com.altech.ledger.endpoint;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.fx.CreateFxRateUseCase;
import com.altech.ledger.usecase.fx.QueryFxRateUseCase;
import com.altech.ledger.usecase.fx.UpdateFxRateUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.altech.ledger.entity.dto.request.CreateFxRateRequestDto;
import com.altech.ledger.entity.dto.response.GetFxRateResponseDto;

@RestController
@RequestMapping("/fx-rates")
@RequiredArgsConstructor
public class FxRateEndpoint {
    private final CreateFxRateUseCase createFxRateUseCase;
    private final UpdateFxRateUseCase updateFxRateUseCase;
    private final QueryFxRateUseCase queryFxRateUseCase;

    @PostMapping
    public Result<GetFxRateResponseDto> create(@Valid @RequestBody CreateFxRateRequestDto dto) {
        return R.success(createFxRateUseCase.execute(dto));
    }

    @PutMapping("/{id}")
    public Result<GetFxRateResponseDto> update(@PathVariable Long id, @Valid @RequestBody CreateFxRateRequestDto dto) {
        return R.success(updateFxRateUseCase.execute(id, dto));
    }

    @GetMapping("/{id}")
    public Result<GetFxRateResponseDto> getOne(@PathVariable Long id) {
        return R.success(queryFxRateUseCase.one(id));
    }

    @GetMapping
    public Result<List<GetFxRateResponseDto>> getAll(
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) String base,
        @RequestParam(required = false) String target
    ) {
        var page = queryFxRateUseCase.list(pageable, base, target);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
