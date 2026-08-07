package com.altech.ledger.usecase.ledger;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;

import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.log.LedgerEntry;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerEntryRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.service.MovementBus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

/**
 * Port of the-wallet-ledger LedgerMovementUseCase — log movement + create ledger entry legs.
 */
@Service
@RequiredArgsConstructor
public class LedgerMovementUseCase {
    private final LedgerMovementRepository movements;
    private final LedgerEntryRepository entries;

    @Transactional
    public LedgerMovement log(LedgerMovementEvent event) {
        if (event.getMovementKey() != null) {
            var existing = movements.findByMovementKey(event.getMovementKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (event.getBelongToWalletId() == null) {
            throw new BizException(WalletErrorResponse.WAL0400, "belongToWalletId required");
        }
        if (event.getOrderType() == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "orderType required");
        }
        String key = event.getMovementKey() == null
            ? "mv-" + UUID.randomUUID() : event.getMovementKey();
        LedgerMovementMode mode = event.getMode() == null ? LedgerMovementMode.AUTO : event.getMode();
        LedgerMovement m = new LedgerMovement(
            key,
            event.getBelongToWalletId(),
            event.getOrderType(),
            mode,
            event.getOriginatorId(),
            event.getTargetId(),
            event.getAmount(),
            event.getCurrency() == null ? "USD" : event.getCurrency().toUpperCase(),
            event.getDescription());
        m.setStatus(event.getStatus() == null ? LedgerMovementStatus.PROCESSING : event.getStatus());
        if (event.getFiles() != null) m.setFiles(event.getFiles());
        if (event.getMetadata() != null) m.setMetadata(event.getMetadata());
        return movements.save(m);
    }

    @Transactional
    public void createLedgerEntries(LedgerMovement movement, List<EntryLeg> legs) {
        for (EntryLeg leg : legs) {
            entries.save(new LedgerEntry(
                movement.getId(),
                leg.targetId(),
                leg.amount(),
                leg.direction(),
                movement.getCurrency()));
        }
    }

    @Transactional(readOnly = true)
    public LedgerMovement get(Long id) {
        return movements.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Movement not found: " + id));
    }

    public record EntryLeg(String targetId, BigDecimal amount, MovementDirection direction) {}
}
