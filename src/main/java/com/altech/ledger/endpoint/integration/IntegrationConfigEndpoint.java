package com.altech.ledger.endpoint.integration;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.UpdateIntegrationConfigRequestDto;
import com.altech.ledger.entity.dto.response.GetIntegrationConfigResponseDto;
import com.altech.ledger.usecase.integration.IntegrationConfigUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration runtime config (auto-wallet, kill-switch) — DB-backed.
 */
@RestController
@RequestMapping("/integrations/config")
@RequiredArgsConstructor
public class IntegrationConfigEndpoint {
    private final IntegrationConfigUseCase integrationConfigUseCase;

    @GetMapping
    public Result<GetIntegrationConfigResponseDto> get() {
        return R.success(integrationConfigUseCase.getOrCreate());
    }

    @PutMapping
    public Result<GetIntegrationConfigResponseDto> update(@Valid @RequestBody UpdateIntegrationConfigRequestDto body) {
        return R.success(integrationConfigUseCase.update(body));
    }
}
