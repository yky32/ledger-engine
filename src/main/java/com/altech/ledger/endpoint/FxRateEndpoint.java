package com.altech.ledger.endpoint;

import com.altech.ledger.entity.dto.parity.ParityDtos.CreateFxRateRequest;
import com.altech.ledger.entity.dto.parity.ParityDtos.FxRateResponse;
import com.altech.ledger.usecase.FxRateQueryUseCase;
import com.altech.ledger.usecase.FxRateSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/fx-rates")
@RequiredArgsConstructor
public class FxRateEndpoint {
    private final FxRateSetupUseCase setupUseCase;
    private final FxRateQueryUseCase queryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FxRateResponse create(@Valid @RequestBody CreateFxRateRequest dto) {
        return setupUseCase.create(dto);
    }

    @PutMapping("/{id}")
    public FxRateResponse update(@PathVariable Long id, @Valid @RequestBody CreateFxRateRequest dto) {
        return setupUseCase.update(id, dto);
    }

    @GetMapping("/{id}")
    public FxRateResponse getOne(@PathVariable Long id) {
        return queryUseCase.getOne(id);
    }

    @GetMapping
    public Page<FxRateResponse> getAll(
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) String base,
        @RequestParam(required = false) String target
    ) {
        return queryUseCase.getAll(pageable, base, target);
    }
}
