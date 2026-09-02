package com.altech.ledger.endpoint.coa;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.CreateCoaDictionaryRequestDto;
import com.altech.ledger.entity.dto.request.UpdateCoaDictionaryRequestDto;
import com.altech.ledger.entity.dto.response.GetCoaDictionaryResponseDto;
import com.altech.ledger.usecase.coa.CoaDictionaryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * COA dictionary — stored definitions for 01, 02, 01-02, 01-02-01.
 */
@RestController
@RequestMapping("/coa-dictionary")
@RequiredArgsConstructor
public class CoaDictionaryEndpoint {
    private final CoaDictionaryUseCase coaDictionaryUseCase;

    @GetMapping
    public Result<List<GetCoaDictionaryResponseDto>> list() {
        return R.success(coaDictionaryUseCase.list());
    }

    @PostMapping("/ensure")
    public Result<List<GetCoaDictionaryResponseDto>> ensure() {
        return R.success(coaDictionaryUseCase.ensure());
    }

    @GetMapping("/{id}")
    public Result<GetCoaDictionaryResponseDto> get(@PathVariable Long id) {
        return R.success(coaDictionaryUseCase.get(id));
    }

    @PostMapping
    public Result<GetCoaDictionaryResponseDto> create(@Valid @RequestBody CreateCoaDictionaryRequestDto body) {
        return R.success(coaDictionaryUseCase.create(body));
    }

    @PutMapping("/{id}")
    public Result<GetCoaDictionaryResponseDto> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCoaDictionaryRequestDto body
    ) {
        return R.success(coaDictionaryUseCase.update(id, body));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        coaDictionaryUseCase.delete(id);
        return R.success();
    }
}
