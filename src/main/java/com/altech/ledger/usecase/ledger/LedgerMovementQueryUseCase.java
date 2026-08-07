package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ledger movement query surface (parity).
 */
@Component
@RequiredArgsConstructor
public class LedgerMovementQueryUseCase {
    private final LedgerMovementRepository movements;
    private final WalletRepository wallets;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public LedgerMovementDtos.Response one(Long id) {
        return DtoMapper.toMovement(commonUseCase.requireMovement(id));
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> list(Pageable pageable, Instant startDt, Instant endDt,
                                                  List<String> statuses) {
        return _filter(movements.findAll(pageable), startDt, endDt, statuses);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> myMovements(String ownerId, Pageable pageable,
                                                         Instant startDt, Instant endDt, List<String> statuses) {
        List<Long> ids = wallets.findByOwnerId(ownerId).stream().map(Wallet::getId).toList();
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        return _filter(movements.findByWalletIdIn(ids, pageable), startDt, endDt, statuses);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> byWallet(Long walletId, Pageable pageable) {
        return movements.findByWalletId(walletId, pageable).map(DtoMapper::toMovement);
    }

    private Page<LedgerMovementDtos.Response> _filter(Page<LedgerMovement> page, Instant startDt, Instant endDt,
                                                      List<String> statuses) {
        Set<LedgerMovementStatus> statusSet = _parseStatuses(statuses);
        List<LedgerMovementDtos.Response> filtered = page.getContent().stream()
            .filter(m -> startDt == null || m.getCreateDt() == null || !m.getCreateDt().isBefore(startDt))
            .filter(m -> endDt == null || m.getCreateDt() == null || !m.getCreateDt().isAfter(endDt))
            .filter(m -> statusSet == null || statusSet.contains(m.getStatus()))
            .map(DtoMapper::toMovement)
            .toList();
        return new PageImpl<>(filtered, page.getPageable(),
            statusSet == null && startDt == null && endDt == null ? page.getTotalElements() : filtered.size());
    }

    private Set<LedgerMovementStatus> _parseStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return statuses.stream()
            .flatMap(s -> Arrays.stream(s.split(",")))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(LedgerMovementStatus::valueOf)
            .collect(Collectors.toSet());
    }
}

