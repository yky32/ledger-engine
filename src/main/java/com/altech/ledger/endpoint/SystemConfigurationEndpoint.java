package com.altech.ledger.endpoint;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.SystemConfigurationUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.SystemDtos;

@RestController
@RequestMapping("/configurations")
@RequiredArgsConstructor
public class SystemConfigurationEndpoint {
    private final SystemConfigurationUseCase useCase;

    @GetMapping
    public Result<SystemDtos.ConfigurationResponse> get(
        @RequestParam String target,
        @RequestParam(required = false, defaultValue = "global") String scope
    ) {
        return R.success(useCase.myConfigurations(target, scope));
    }

    @PutMapping
    public Result<SystemDtos.ConfigurationResponse> upsert(@RequestBody Map<String, String> body) {
        return R.success(useCase.upsert(
            body.getOrDefault("name", body.get("target")),
            body.get("target"),
            body.getOrDefault("scope", "global"),
            body.get("value")));
    }
}
