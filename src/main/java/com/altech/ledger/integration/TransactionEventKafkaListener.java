package com.altech.ledger.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ledger.integration.kafka.enabled", havingValue = "true")
public class TransactionEventKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final TransactionIngestionService ingestionService;

    public TransactionEventKafkaListener(ObjectMapper objectMapper, TransactionIngestionService ingestionService) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
    }

    @KafkaListener(topics = "${ledger.integration.kafka.topic}", groupId = "${ledger.integration.kafka.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            TransactionalEvent event = objectMapper.readValue(record.value(), TransactionalEvent.class);
            IngestionResult result = ingestionService.ingest(event);
            log.info("Kafka event {} -> {}", event.eventId(), result.status());
        } catch (Exception ex) {
            log.error("Failed to process Kafka record offset={}: {}", record.offset(), ex.getMessage(), ex);
        }
    }
}
