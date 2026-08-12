package com.altech.ledger.endpoint;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.GetSystemConfigurationResponseDto;
import com.altech.ledger.usecase.config.QuerySystemConfigurationUseCase;
import com.altech.ledger.usecase.config.UpsertSystemConfigurationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    /**
     * Upsert config. {@code value} may be string, number, object, or array (stored as JSONB).
     * Example name: {@code user-register.otp}
     */
    @PutMapping
    public Result<GetSystemConfigurationResponseDto> upsert(@RequestBody Map<String, Object> body) {
        String target = body.get("target") == null ? null : String.valueOf(body.get("target"));
        String scope = body.get("scope") == null ? "global" : String.valueOf(body.get("scope"));
        String name = body.get("name") == null ? target : String.valueOf(body.get("name"));
        Object value = body.get("value");
        return R.success(upsertSystemConfigurationUseCase.execute(name, target, scope, value));
    }
}
