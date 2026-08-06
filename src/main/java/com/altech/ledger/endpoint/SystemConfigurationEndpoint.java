package com.altech.ledger.endpoint;

import com.altech.ledger.usecase.SystemConfigurationUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.system.SystemDtos;

@RestController
@RequestMapping("/configurations")
@RequiredArgsConstructor
public class SystemConfigurationEndpoint {
    private final SystemConfigurationUseCase useCase;

    @GetMapping
    public SystemDtos.ConfigurationResponse get(
        @RequestParam String target,
        @RequestParam(required = false, defaultValue = "global") String scope
    ) {
        return useCase.myConfigurations(target, scope);
    }

    @PutMapping
    public SystemDtos.ConfigurationResponse upsert(@RequestBody Map<String, String> body) {
        return useCase.upsert(
            body.getOrDefault("name", body.get("target")),
            body.get("target"),
            body.getOrDefault("scope", "global"),
            body.get("value"));
    }
}
