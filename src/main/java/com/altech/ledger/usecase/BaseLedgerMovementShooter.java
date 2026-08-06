package com.altech.ledger.usecase;

import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.service.MovementBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of the-wallet-ledger BaseLedgerMovementShooter.
 * Handles mode branching (AUTO vs MANUAL) before dispatch.
 */
public abstract class BaseLedgerMovementShooter {
    private static final Logger log = LoggerFactory.getLogger(BaseLedgerMovementShooter.class);

    private final MovementBus movementBus;
    private final LedgerMovementRepository movements;
    private final LedgerMovementMode defaultMode;

    protected BaseLedgerMovementShooter(MovementBus movementBus, LedgerMovementRepository movements,
                                        LedgerMovementMode defaultMode) {
        this.movementBus = movementBus;
        this.movements = movements;
        this.defaultMode = defaultMode == null ? LedgerMovementMode.AUTO : defaultMode;
    }

    protected LedgerMovement execute(LedgerMovement movement) {
        beforeExecute(movement);
        if (movement.getMode() == null) {
            // mode set at construction; no-op
        }
        log.info("BaseLedgerMovementShooter.execute id={} mode={} orderType={}",
            movement.getId(), movement.getMode(), movement.getOrderType());

        switch (movement.getMode() == null ? defaultMode : movement.getMode()) {
            case AUTO -> {
                movement.setStatus(LedgerMovementStatus.PROCESSING);
                movements.save(movement);
                return movementBus.dispatch(movement);
            }
            case MANUAL -> {
                if (movement.getOrderType() != null
                    && movement.getOrderType().name().equals("DEPOSIT")) {
                    movement.setStatus(LedgerMovementStatus.PENDING_DOCS);
                } else {
                    movement.setStatus(LedgerMovementStatus.PROCESSING);
                }
                movements.save(movement);
                // MANUAL does not settle until settle/status API
                return movement;
            }
            default -> {
                movement.setStatus(LedgerMovementStatus.PROCESSING);
                movements.save(movement);
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

    protected LedgerMovementRepository movements() {
        return movements;
    }
}
