package com.altech.ledger.service;

import com.altech.ledger.config.MovementKafkaProperties;
import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.usecase.ledger.LedgerMovementExecutionUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Dispatches movements: sync execute (default) or Kafka MOVEMENT_INITIATED when enabled.
 */
@Service
public class MovementBus {
    private static final Logger log = LoggerFactory.getLogger(MovementBus.class);

    private final MovementKafkaProperties kafkaProperties;
    private final LedgerMovementExecutionUseCase execution;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate;

    public MovementBus(MovementKafkaProperties kafkaProperties,
                       LedgerMovementExecutionUseCase execution,
                       ObjectMapper objectMapper,
                       ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate) {
        this.kafkaProperties = kafkaProperties;
        this.execution = execution;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public LedgerMovement dispatch(LedgerMovement movement) {
        if (movement.getMode() != LedgerMovementMode.AUTO) {
            return movement;
        }
        if (kafkaProperties.isEnabled() && kafkaTemplate.getIfAvailable() != null) {
            try {
                LedgerMovementEvent event = toEvent(movement);
                String json = objectMapper.writeValueAsString(event);
                kafkaTemplate.getObject().send(kafkaProperties.getInitiatedTopic(),
                    String.valueOf(movement.getId()), json);
                log.info("Published MOVEMENT_INITIATED id={}", movement.getId());
                return movement;
            } catch (Exception ex) {
                log.warn("Kafka publish failed; falling back to sync execute: {}", ex.getMessage());
            }
        }
        return execution.execute(movement);
    }

    public void publishDone(LedgerMovement movement) {
        if (!kafkaProperties.isEnabled() || kafkaTemplate.getIfAvailable() == null) {
            return;
        }
        try {
            LedgerMovementEvent event = toEvent(movement);
            event.setStatus(LedgerMovementStatus.SETTLED);
            kafkaTemplate.getObject().send(kafkaProperties.getDoneTopic(),
                String.valueOf(movement.getId()), objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("Kafka DONE publish failed: {}", ex.getMessage());
        }
    }

    public static LedgerMovementEvent toEvent(LedgerMovement m) {
        LedgerMovementEvent e = new LedgerMovementEvent();
        e.setMovementId(m.getId());
        e.setMovementKey(m.getMovementKey());
        e.setBelongToWalletId(m.getWalletId());
        e.setOriginatorId(m.getOriginatorId());
        e.setTargetId(m.getTargetId());
        e.setAmount(m.getAmount());
        e.setCurrency(m.getCurrency());
        e.setOrderType(m.getOrderType());
        e.setMode(m.getMode());
        e.setStatus(m.getStatus());
        e.setDescription(m.getMetadata());
        e.setFiles(m.getFiles());
        e.setMetadata(m.getMetadata());
        return e;
    }
}
