package com.altech.ledger.endpoint.integration;

import com.altech.core.exception.BizException;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.UseCaseCatalogItemDto;
import com.altech.ledger.exception.response.CoaErrorResponse;
import com.altech.ledger.usecase.catalog.ListUseCaseCatalogUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Upstream-facing catalog: ops-configured use cases (Brain + COA + recipe).
 * SDK: {@code client.catalog().listUseCases()}.
 */
@RestController
@RequestMapping("/integrations/use-cases")
@RequiredArgsConstructor
public class UseCaseCatalogEndpoint {
    private final ListUseCaseCatalogUseCase listUseCaseCatalogUseCase;

    /**
     * @param enabledOnly default true — only invokable (enabled Brain rule) rows
     */
    @GetMapping
    public Result<List<UseCaseCatalogItemDto>> list(
        @RequestParam(required = false, defaultValue = "true") boolean enabledOnly
    ) {
        return R.success(listUseCaseCatalogUseCase.list(enabledOnly));
    }

    @GetMapping("/{code}")
    public Result<UseCaseCatalogItemDto> get(@PathVariable String code) {
        return R.success(listUseCaseCatalogUseCase.find(code)
            .orElseThrow(() -> new BizException(CoaErrorResponse.COA0404, "useCase=" + code)));
    }
}
