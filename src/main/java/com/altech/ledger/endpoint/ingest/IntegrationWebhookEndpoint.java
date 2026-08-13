package com.altech.ledger.endpoint.ingest;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.ingest.IngestionResult;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.usecase.ingest.IngestTransactionUseCase;
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

    /** Live ingest: Door → Brain → wallet → books. Result includes eligibilityTrace. */
    @PostMapping("/transactions")
    public Result<IngestionResult> receive(@Valid @RequestBody TransactionalEvent event) {
        return R.success(ingestTransactionUseCase.execute(event));
    }

    /**
     * Trust pack B — dry-run: same Brain match + points, no wallet / movements / fail-row.
     * {@code data.dryRun=true}.
     */
    @PostMapping("/transactions/dry-run")
    public Result<IngestionResult> dryRun(@Valid @RequestBody TransactionalEvent event) {
        return R.success(ingestTransactionUseCase.dryRun(event));
    }
}
