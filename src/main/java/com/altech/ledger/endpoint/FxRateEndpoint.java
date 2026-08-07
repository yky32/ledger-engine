package com.altech.ledger.endpoint;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import java.util.List;

import com.altech.ledger.usecase.FxRateQueryUseCase;
import com.altech.ledger.usecase.FxRateSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.FxRateDtos;

@RestController
@RequestMapping("/fx-rates")
@RequiredArgsConstructor
public class FxRateEndpoint {
    private final FxRateSetupUseCase setupUseCase;
    private final FxRateQueryUseCase queryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<FxRateDtos.Response> create(@Valid @RequestBody FxRateDtos.CreateRequest dto) {
        return R.success(setupUseCase.create(dto));
    }

    @PutMapping("/{id}")
    public Result<FxRateDtos.Response> update(@PathVariable Long id, @Valid @RequestBody FxRateDtos.CreateRequest dto) {
        return R.success(setupUseCase.update(id, dto));
    }

    @GetMapping("/{id}")
    public Result<FxRateDtos.Response> getOne(@PathVariable Long id) {
        return R.success(queryUseCase.getOne(id));
    }

    @GetMapping
    public Result<List<FxRateDtos.Response>> getAll(
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) String base,
        @RequestParam(required = false) String target
    ) {
        return R.success(queryUseCase.getAll(pageable, base, target));
    }
}
