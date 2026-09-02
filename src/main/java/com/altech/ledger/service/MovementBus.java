package com.altech.ledger.service;

import com.altech.ledger.config.MovementKafkaProperties;
import com.altech.ledger.entity.dto.event.BalanceUpdatedEvent;
import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.usecase.ledger.LedgerMovementExecutionUseCase;
import com.altech.core.utils.JSONUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispatches movements: sync execute (default) or Kafka MOVEMENT_INITIATED when enabled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovementBus {
    private final MovementKafkaProperties movementKafkaProperties;
    private final LedgerMovementExecutionUseCase ledgerMovementExecutionUseCase;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate;

    public LedgerMovement dispatch(LedgerMovement movement) {
        if (movement.getMode() != LedgerMovementMode.AUTO) {
            return movement;
        }
        if (movementKafkaProperties.isEnabled() && kafkaTemplate.getIfAvailable() != null) {
            try {
                LedgerMovementEvent event = toEvent(movement, "LEDGER_MOVEMENT_INITIATED");
                String json = JSONUtil.writeValue(event);
                kafkaTemplate.getObject().send(movementKafkaProperties.getInitiatedTopic(),
                    String.valueOf(movement.getId()), json);
                log.info("Published MOVEMENT_INITIATED id={} requestId={}", movement.getId(), event.getRequestId());
                return movement;
            } catch (Exception ex) {
                log.warn("Kafka publish failed; falling back to sync execute: {}", ex.getMessage());
            }
        }
        return ledgerMovementExecutionUseCase.execute(movement);
    }

    public void publishDone(LedgerMovement movement) {
        if (!movementKafkaProperties.isEnabled() || kafkaTemplate.getIfAvailable() == null) {
            return;
        }
        try {
            LedgerMovementEvent event = toEvent(movement, "LEDGER_MOVEMENT_DONE");
            event.setStatus(LedgerMovementStatus.SETTLED);
            kafkaTemplate.getObject().send(movementKafkaProperties.getDoneTopic(),
                String.valueOf(movement.getId()), JSONUtil.writeValue(event));
            log.info("Published MOVEMENT_DONE id={} topic={}",
                movement.getId(), movementKafkaProperties.getDoneTopic());
        } catch (Exception ex) {
            log.warn("Kafka DONE publish failed: {}", ex.getMessage());
        }
    }

    /**
     * Fire after account balances are updated. Downstream CRM/notify/points UI subscribe here.
     */
    public void publishBalanceUpdated(BalanceUpdatedEvent event) {
        if (event == null) {
            return;
        }
        if (!movementKafkaProperties.isEnabled() || kafkaTemplate.getIfAvailable() == null) {
            log.debug("Kafka balance-updated skipped (disabled) movementId={}", event.getMovementId());
            return;
        }
        try {
            if (event.getEventName() == null || event.getEventName().isBlank()) {
                event.setEventName("LEDGER_BALANCE_UPDATED");
            }
            String key = event.getWalletId() != null
                ? String.valueOf(event.getWalletId())
                : String.valueOf(event.getMovementId());
            String json = JSONUtil.writeValue(event);
            kafkaTemplate.getObject().send(
                movementKafkaProperties.getBalanceUpdatedTopic(), key, json);
            log.info("Published BALANCE_UPDATED movementId={} walletId={} ownerId={} topic={}",
                event.getMovementId(), event.getWalletId(), event.getOwnerId(),
                movementKafkaProperties.getBalanceUpdatedTopic());
        } catch (Exception ex) {
            log.warn("Kafka BALANCE_UPDATED publish failed: {}", ex.getMessage());
        }
    }

    public static LedgerMovementEvent toEvent(LedgerMovement m) {
        return toEvent(m, "LEDGER_MOVEMENT");
    }

    public static LedgerMovementEvent toEvent(LedgerMovement m, String eventName) {
        LedgerMovementEvent e = new LedgerMovementEvent();
        e.setEventName(eventName);
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

