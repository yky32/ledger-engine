package com.altech.ledger.usecase;

import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.service.MovementBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * BaseLedgerMovementShooter.
 * Handles mode branching (AUTO vs MANUAL) before dispatch.
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseLedgerMovementShooter {
    private final MovementBus movementBus;
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerMovementMode ledgerMovementMode;

    protected LedgerMovement execute(LedgerMovement movement) {
        beforeExecute(movement);
        if (movement.getMode() == null) {
            // mode set at construction; no-op
        }
        log.info("BaseLedgerMovementShooter.execute id={} mode={} orderType={}",
            movement.getId(), movement.getMode(), movement.getOrderType());

        switch (movement.getMode() == null ? ledgerMovementMode : movement.getMode()) {
            case AUTO -> {
                movement.setStatus(LedgerMovementStatus.PROCESSING);
                ledgerMovementRepository.save(movement);
                return movementBus.dispatch(movement);
            }
            case MANUAL -> {
                if (movement.getOrderType() != null
                    && movement.getOrderType().name().equals("DEPOSIT")) {
                    movement.setStatus(LedgerMovementStatus.PENDING_DOCS);
                } else {
                    movement.setStatus(LedgerMovementStatus.PROCESSING);
                }
                ledgerMovementRepository.save(movement);
                // MANUAL does not settle until settle/status API
                return movement;
            }
            default -> {
                movement.setStatus(LedgerMovementStatus.PROCESSING);
                ledgerMovementRepository.save(movement);
                return movementBus.dispatch(movement);
            }
        }
    }

    protected void beforeExecute(LedgerMovement movement) {
        // hook for subclasses (txn id / alias generation, etc.)
    }

    protected void reconcile(LedgerMovementEvent event) {
        // hook for subclasses
    }

    protected MovementBus movementBus() {
        return movementBus;
    }

    protected LedgerMovementRepository ledgerMovementRepository() {
        return ledgerMovementRepository;
    }
}
