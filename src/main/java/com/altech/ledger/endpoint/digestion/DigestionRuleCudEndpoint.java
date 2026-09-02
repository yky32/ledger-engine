package com.altech.ledger.endpoint.digestion;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.CreateDigestionRuleRequestDto;
import com.altech.ledger.entity.dto.request.UpdateDigestionRuleRequestDto;
import com.altech.ledger.entity.dto.response.GetDigestionRuleResponseDto;
import com.altech.ledger.usecase.digestion.DigestionRuleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Digestion rules CUD — runtime earn/scoring config (no restart).
 */
@RestController
@RequestMapping("/digestion-rules")
@RequiredArgsConstructor
public class DigestionRuleCudEndpoint {
    private final DigestionRuleUseCase digestionRuleUseCase;

    @PostMapping
    public Result<GetDigestionRuleResponseDto> create(@Valid @RequestBody CreateDigestionRuleRequestDto body) {
        return R.success(digestionRuleUseCase.create(body));
    }

    @PutMapping("/{id}")
    public Result<GetDigestionRuleResponseDto> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateDigestionRuleRequestDto body
    ) {
        return R.success(digestionRuleUseCase.update(id, body));
    }

    @PostMapping("/{id}/enable")
    public Result<GetDigestionRuleResponseDto> enable(@PathVariable Long id) {
        return R.success(digestionRuleUseCase.setEnabled(id, true));
    }

    @PostMapping("/{id}/disable")
    public Result<GetDigestionRuleResponseDto> disable(@PathVariable Long id) {
        return R.success(digestionRuleUseCase.setEnabled(id, false));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        digestionRuleUseCase.delete(id);
        return R.success();
    }
}
