package com.altech.ledger.listener;

import com.altech.core.utils.JSONUtil;
import com.altech.ledger.entity.dto.ingest.IngestionResult;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.usecase.ingest.IngestTransactionUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Async twin of {@code POST /integrations/webhooks/transactions}.
 * Same SDK JSON ({@link TransactionalEvent}); same Door → Brain → books use case.
 * Enable with {@code LEDGER_KAFKA_ENABLED=true}. Topic default {@code ledger.transaction.events}.
 */
@Component
@ConditionalOnProperty(name = "ledger.integration.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TransactionEventKafkaListener {
    private final IngestTransactionUseCase ingestTransactionUseCase;

    @KafkaListener(
        topics = "${ledger.integration.kafka.topic:ledger.transaction.events}",
        groupId = "${ledger.integration.kafka.group-id:ledger-engine}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        String json = record.value();
        try {
            TransactionalEvent event = JSONUtil.readValue(json, TransactionalEvent.class);
            IngestionResult result = ingestTransactionUseCase.execute(event);
            log.info("Kafka ingest topic={} key={} eventId={} ownerId={} status={}",
                record.topic(), record.key(), event.eventId(), event.ownerId(), result.status());
        } catch (Exception ex) {
            // Parse / unexpected errors: log and skip (do not poison the consumer).
            // Door/Brain skips already persist on the fail queue inside execute().
            log.error("Kafka ingest failed topic={} offset={} payload={} err={}",
                record.topic(), record.offset(), preview(json), ex.getMessage(), ex);
        }
    }

    private static String preview(String json) {
        if (json == null) {
            return "null";
        }
        String t = json.replaceAll("\\s+", " ").trim();
        return t.length() <= 400 ? t : t.substring(0, 400) + "…";
    }
}
