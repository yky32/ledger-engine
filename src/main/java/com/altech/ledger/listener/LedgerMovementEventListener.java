package com.altech.ledger.listener;

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
 * LedgerMovementEventListener (BALANCE_UPDATE → execute).
 * Uses same topic as initiated when only one bus is configured, or dedicated balance-update topic.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.movement.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class LedgerMovementEventListener {
    private final LedgerMovementExecutionUseCase ledgerMovementExecutionUseCase;

    @KafkaListener(
        topics = {
            "${ledger.movement.kafka.balance-update-topic:ledger.movement.balance-update}",
            "${ledger.movement.kafka.initiated-topic:ledger.movement.initiated}"
        },
        groupId = "${ledger.movement.kafka.group-id:ledger-engine-movement}-balance"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            LedgerMovementEvent event = JSONUtil.readValue(record.value(), LedgerMovementEvent.class);
            log.info("BALANCE_UPDATE movementId={} topic={}", event.getMovementId(), record.topic());
            ledgerMovementExecutionUseCase.execute(event);
        } catch (Exception ex) {
            log.error("Failed BALANCE_UPDATE: {}", ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }
}
