package com.altech.ledger.listener.log;

import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.core.utils.JSONUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * LedgerMovementDoneListener — BALANCE_UPDATE_DONE log sink.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.movement.kafka", name = "enabled", havingValue = "true")
public class LedgerMovementDoneListener {
    private static final Logger log = LoggerFactory.getLogger(LedgerMovementDoneListener.class);

    @KafkaListener(
        topics = "${ledger.movement.kafka.done-topic:ledger.movement.done}",
        groupId = "${ledger.movement.kafka.group-id:ledger-engine-movement}-done"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            LedgerMovementEvent event = JSONUtil.readValue(record.value(), LedgerMovementEvent.class);
            log.info("BALANCE_UPDATE_DONE movementId={} status={} amount={} {}",
                event.getMovementId(), event.getStatus(), event.getAmount(), event.getCurrency());
        } catch (Exception ex) {
            log.error("Failed processing DONE: {}", ex.getMessage(), ex);
        }
    }
}
