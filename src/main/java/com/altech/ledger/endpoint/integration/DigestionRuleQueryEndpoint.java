package com.altech.ledger.endpoint.integration;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.GetDigestionRuleResponseDto;
import com.altech.ledger.usecase.integration.DigestionRuleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Digestion rules Query (R).
 * <p>
 * Filters via query params — no {@code /by-*} paths.
 * {@code GET /digestion-rules?code=} returns one rule; without {@code code} returns a list.
 */
@RestController
@RequestMapping("/digestion-rules")
@RequiredArgsConstructor
public class DigestionRuleQueryEndpoint {
    private final DigestionRuleUseCase digestionRuleUseCase;

    @GetMapping
    public Result<?> listOrByCode(
        @RequestParam(required = false) Boolean enabledOnly,
        @RequestParam(required = false) String code
    ) {
        if (code != null && !code.isBlank()) {
            return R.success(digestionRuleUseCase.getByCode(code.trim()));
        }
        return R.success(digestionRuleUseCase.list(enabledOnly));
    }

    @GetMapping("/{id}")
    public Result<GetDigestionRuleResponseDto> one(@PathVariable Long id) {
        return R.success(digestionRuleUseCase.get(id));
    }
}
