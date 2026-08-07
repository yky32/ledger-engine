package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.ledger.LedgerDto.PageResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class QueryMovementUseCase {
    private final LedgerMovementRepository ledgerMovementRepository;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public MovementResponse one(Long id) {
        return _response(commonUseCase.requireMovement(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> listByWallet(Long walletId, Pageable pageable) {
        commonUseCase.requireWallet(walletId);
        Page<LedgerMovement> page = ledgerMovementRepository.findByWalletId(walletId, pageable);
        return new PageResponse<>(page.map(this::_response).getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    private MovementResponse _response(LedgerMovement movement) {
        return new MovementResponse(movement.getId(), movement.getMovementKey(), movement.getWalletId(),
            movement.getOrderType(), movement.getStatus(), movement.getMode(), movement.getOriginatorId(),
            movement.getTargetId(), movement.getAmount(), movement.getCurrency(),
            movement.getCreateDt(), movement.getUpdateDt());
    }
}
