package com.altech.ledger.endpoint.integration;

import com.altech.ledger.entity.dto.integration.IngestionResult;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.usecase.integration.TransactionIngestionUseCase;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integrations/webhooks")
@ConditionalOnProperty(name = "ledger.integration.enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationWebhookEndpoint {
    private final TransactionIngestionUseCase ingestionUseCase;

    public IntegrationWebhookEndpoint(TransactionIngestionUseCase ingestionUseCase) {
        this.ingestionUseCase = ingestionUseCase;
    }

    @PostMapping("/transactions")
    public ResponseEntity<IngestionResult> receive(@Valid @RequestBody TransactionalEvent event) {
        return ResponseEntity.ok(ingestionUseCase.ingest(event));
    }
}
