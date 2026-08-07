package com.altech.ledger.listener;

import com.altech.ledger.entity.dto.integration.IngestionResult;
import com.altech.ledger.entity.dto.integration.TransactionalEvent;
import com.altech.ledger.usecase.integration.IngestTransactionUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "ledger.integration.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TransactionEventKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final IngestTransactionUseCase ingestionUseCase;

    @KafkaListener(topics = "${ledger.integration.kafka.topic}", groupId = "${ledger.integration.kafka.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            TransactionalEvent event = objectMapper.readValue(record.value(), TransactionalEvent.class);
            IngestionResult result = ingestionUseCase.execute(event);
            log.info("Kafka event {} -> {}", event.eventId(), result.status());
        } catch (Exception ex) {
            log.error("Failed to process Kafka record offset={}: {}", record.offset(), ex.getMessage(), ex);
        }
    }
}
