package com.altech.ledger.service;

import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.LedgerMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerMovementService {
    private final LedgerMovementRepository movements;

    public LedgerMovementService(LedgerMovementRepository movements) {
        this.movements = movements;
    }

    @Transactional(readOnly = true)
    public LedgerMovement get(Long id) {
        return movements.findById(id)
            .orElseThrow(() -> LedgerException.notFound("Movement not found: " + id));
    }

    @Transactional(readOnly = true)
    public LedgerMovement getByKey(String movementKey) {
        return movements.findByMovementKey(movementKey)
            .orElseThrow(() -> LedgerException.notFound("Movement not found key: " + movementKey));
    }

    @Transactional
    public LedgerMovement save(LedgerMovement movement) {
        return movements.save(movement);
    }
}
