package com.altech.ledger.endpoint.coa;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.CreateCoaProfileRequestDto;
import com.altech.ledger.entity.dto.request.UpdateCoaProfileRequestDto;
import com.altech.ledger.entity.dto.response.GetCoaProfileResponseDto;
import com.altech.ledger.entity.dto.response.UafDemoSeedResponseDto;
import com.altech.ledger.usecase.coa.CoaProfileUseCase;
import com.altech.ledger.usecase.coa.UafDemoSeedUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/coa-profiles")
@RequiredArgsConstructor
public class CoaProfileEndpoint {
    private final CoaProfileUseCase coaProfileUseCase;
    private final UafDemoSeedUseCase uafDemoSeedUseCase;

    @GetMapping
    public Result<List<GetCoaProfileResponseDto>> list() {
        return R.success(coaProfileUseCase.list());
    }

    @GetMapping("/default")
    public Result<GetCoaProfileResponseDto> getDefault() {
        return R.success(coaProfileUseCase.getOrCreateDefault());
    }

    /** Idempotent UAF_CC + UAF_LOAN profiles + two demo wallets. */
    @PostMapping("/uaf-demo-seed")
    public Result<UafDemoSeedResponseDto> seedUafDemo() {
        return R.success(uafDemoSeedUseCase.execute());
    }

    @GetMapping(params = "code")
    public Result<GetCoaProfileResponseDto> getByCode(@RequestParam String code) {
        return R.success(coaProfileUseCase.getByCode(code));
    }

    @GetMapping("/{id}")
    public Result<GetCoaProfileResponseDto> get(@PathVariable Long id) {
        return R.success(coaProfileUseCase.get(id));
    }

    @PostMapping
    public Result<GetCoaProfileResponseDto> create(@Valid @RequestBody CreateCoaProfileRequestDto body) {
        return R.success(coaProfileUseCase.create(body));
    }

    @PutMapping("/{id}")
    public Result<GetCoaProfileResponseDto> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCoaProfileRequestDto body
    ) {
        return R.success(coaProfileUseCase.update(id, body));
    }
}

