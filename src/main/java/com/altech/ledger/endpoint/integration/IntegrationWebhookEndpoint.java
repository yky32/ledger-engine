package com.altech.ledger.endpoint.integration;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.integration.IngestionResult;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.usecase.integration.IngestTransactionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integrations/webhooks")
@ConditionalOnProperty(name = "ledger.integration.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class IntegrationWebhookEndpoint {
    private final IngestTransactionUseCase ingestTransactionUseCase;

    @PostMapping("/transactions")
    public Result<IngestionResult> receive(@Valid @RequestBody TransactionalEvent event) {
        return R.success(ingestTransactionUseCase.execute(event));
    }
}
