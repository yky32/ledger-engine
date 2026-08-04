package com.altech.ledger.integration;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/webhooks")
@ConditionalOnProperty(name = "ledger.integration.enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationWebhookController {
    private final TransactionIngestionService ingestionService;

    public IntegrationWebhookController(TransactionIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<IngestionResult> receive(@Valid @RequestBody TransactionalEvent event) {
        return ResponseEntity.ok(ingestionService.ingest(event));
    }
}
