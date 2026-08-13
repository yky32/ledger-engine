package com.altech.ledger.endpoint.ingest;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.UpdateIngestPolicyRequestDto;
import com.altech.ledger.entity.dto.response.GetIngestPolicyResponseDto;
import com.altech.ledger.usecase.ingest.IngestPolicyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingest policy (webhook on/off + auto-wallet defaults) — DB-backed.
 */
@RestController
@RequestMapping("/ingest-policies")
@RequiredArgsConstructor
public class IngestPolicyEndpoint {
    private final IngestPolicyUseCase ingestPolicyUseCase;

    @GetMapping
    public Result<GetIngestPolicyResponseDto> get() {
        return R.success(ingestPolicyUseCase.getOrCreate());
    }

    @PutMapping
    public Result<GetIngestPolicyResponseDto> update(@Valid @RequestBody UpdateIngestPolicyRequestDto body) {
        return R.success(ingestPolicyUseCase.update(body));
    }
}
