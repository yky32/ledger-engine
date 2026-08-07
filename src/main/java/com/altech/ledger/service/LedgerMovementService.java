package com.altech.ledger.service;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;

import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LedgerMovementService {
    private final LedgerMovementRepository movements;

    @Transactional(readOnly = true)
    public LedgerMovement get(Long id) {
        return movements.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Movement not found: " + id));
    }

    @Transactional(readOnly = true)
    public LedgerMovement getByKey(String movementKey) {
        return movements.findByMovementKey(movementKey)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Movement not found key: " + movementKey));
    }

    @Transactional
    public LedgerMovement save(LedgerMovement movement) {
        return movements.save(movement);
    }
}
