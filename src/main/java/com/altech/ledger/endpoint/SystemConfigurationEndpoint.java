package com.altech.ledger.endpoint;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.config.QuerySystemConfigurationUseCase;
import com.altech.ledger.usecase.config.UpsertSystemConfigurationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.altech.ledger.entity.dto.response.GetSystemConfigurationResponseDto;

@RestController
@RequestMapping("/configurations")
@RequiredArgsConstructor
public class SystemConfigurationEndpoint {
    private final QuerySystemConfigurationUseCase querySystemConfigurationUseCase;
    private final UpsertSystemConfigurationUseCase upsertSystemConfigurationUseCase;

    @GetMapping
    public Result<GetSystemConfigurationResponseDto> get(
        @RequestParam String target,
        @RequestParam(required = false, defaultValue = "global") String scope
    ) {
        return R.success(querySystemConfigurationUseCase.execute(target, scope));
    }

    @PutMapping
    public Result<GetSystemConfigurationResponseDto> upsert(@RequestBody Map<String, String> body) {
        return R.success(upsertSystemConfigurationUseCase.execute(
            body.getOrDefault("name", body.get("target")),
            body.get("target"),
            body.getOrDefault("scope", "global"),
            body.get("value")));
    }
}
