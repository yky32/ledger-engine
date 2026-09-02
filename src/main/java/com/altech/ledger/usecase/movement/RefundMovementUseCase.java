package com.altech.ledger.usecase.movement;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.LedgerMovementType;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.ledger.LedgerMovementExecutionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rollback a settled earn/burn from the original movement: negate the amount,
 * post the same double-entry walk with DR/CR swapped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundMovementUseCase {
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerMovementExecutionUseCase ledgerMovementExecutionUseCase;

    @Transactional
    public GetLedgerMovementResponseDto execute(Long originalMovementId) {
        if (originalMovementId == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "movementId required");
        }
        LedgerMovement orig = ledgerMovementRepository.findById(originalMovementId)
            .orElseThrow(() -> new BizException(MovementErrorResponse.MOV0404,
                "Movement not found: " + originalMovementId));

        if (orig.getOrderType() == OrderType.ADJUSTMENT_REFUND) {
            throw new BizException(MovementErrorResponse.MOV0400, "cannot refund a refund");
        }
        if (orig.getOrderType() != OrderType.EARN && orig.getOrderType() != OrderType.BURN) {
            throw new BizException(MovementErrorResponse.MOV0400,
                "refund only for EARN/BURN movements, got " + orig.getOrderType());
        }

        String key = orig.getMovementKey() + "-refund";
        var existing = ledgerMovementRepository.findByMovementKey(key)
            .or(() -> ledgerMovementRepository.findFirstByAssociatedLedgerMovementId(orig.getId()));
        if (existing.isPresent()) {
            return DtoWrapper.getLedgerMovementResponseDto(existing.get());
        }
        if (orig.getStatus() == LedgerMovementStatus.REFUNDED) {
            throw new BizException(MovementErrorResponse.MOV0409, "movement already refunded: " + orig.getId());
        }
        if (orig.getStatus() != LedgerMovementStatus.SETTLED) {
            throw new BizException(MovementErrorResponse.MOV0400,
                "refund requires SETTLED movement, got " + orig.getStatus());
        }

        LedgerMovement refund = new LedgerMovement();
        refund.setMovementKey(key);
        refund.setAlias(key);
        refund.setWalletId(orig.getWalletId());
        refund.setAmount(orig.getAmount() == null ? null : orig.getAmount().negate());
        refund.setCurrency(orig.getCurrency());
        refund.setOrderType(OrderType.ADJUSTMENT_REFUND);
        refund.setMode(LedgerMovementMode.AUTO);
        refund.setType(LedgerMovementType.TRANSFER);
        refund.setStatus(LedgerMovementStatus.PROCESSING);
        refund.setAssociatedLedgerMovementId(orig.getId());
        refund.setEvent(orig.getEvent());
        refund.setMainAccount(orig.getMainAccount());
        refund.setOriginatorId(orig.getOriginatorId());
        refund.setTargetId(orig.getTargetId());
        refund.setMetadata("refund of " + orig.getMovementKey());
        refund.setRemarks("reverse DR/CR of movement " + orig.getId());
        refund = ledgerMovementRepository.save(refund);

        LedgerMovement settled = ledgerMovementExecutionUseCase.execute(refund);

        orig.setStatus(LedgerMovementStatus.REFUNDED);
        orig.setAssociatedLedgerMovementId(settled.getId());
        ledgerMovementRepository.save(orig);

        log.info("refunded movement {} -> {} amount {} -> {}",
            orig.getId(), settled.getId(), orig.getAmount(), settled.getAmount());
        return DtoWrapper.getLedgerMovementResponseDto(settled);
    }
}
