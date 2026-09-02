package com.altech.ledger.listener.log;

import com.altech.ledger.config.MovementKafkaProperties;
import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.usecase.ledger.LedgerMovementExecutionUseCase;
import com.altech.core.utils.JSONUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * LedgerMovementInitiatedListener — MOVEMENT_INITIATED → execute balances.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.movement.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class LedgerMovementInitiatedListener {
    private final LedgerMovementExecutionUseCase ledgerMovementExecutionUseCase;

    @KafkaListener(
        topics = "${ledger.movement.kafka.initiated-topic:ledger.movement.initiated}",
        groupId = "${ledger.movement.kafka.group-id:ledger-engine-movement}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            LedgerMovementEvent event = JSONUtil.readValue(record.value(), LedgerMovementEvent.class);
            log.info("MOVEMENT_INITIATED movementId={}", event.getMovementId());
            ledgerMovementExecutionUseCase.execute(event);
        } catch (Exception ex) {
            log.error("Failed processing MOVEMENT_INITIATED: {}", ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }
}
