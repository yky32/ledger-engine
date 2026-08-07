package com.altech.ledger.usecase.ledger;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;

import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;

/**
 * Port of the-wallet-ledger LedgerMovementQueryUseCase.
 */
@Service
@RequiredArgsConstructor
public class LedgerMovementQueryUseCase {
    private final LedgerMovementRepository movements;
    private final WalletRepository wallets;

    @Transactional(readOnly = true)
    public LedgerMovementDtos.Response getOne(Long id) {
        return DtoMapper.toMovement(movements.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Movement not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> getAll(Pageable pageable, Instant startDt, Instant endDt,
                                         List<String> statuses) {
        return filter(movements.findAll(pageable), startDt, endDt, statuses);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> myMovements(String ownerId, Pageable pageable,
                                              Instant startDt, Instant endDt, List<String> statuses) {
        List<Long> ids = wallets.findByOwnerId(ownerId).stream().map(Wallet::getId).toList();
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        return filter(movements.findByWalletIdIn(ids, pageable), startDt, endDt, statuses);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> byWallet(Long walletId, Pageable pageable) {
        return movements.findByWalletId(walletId, pageable).map(DtoMapper::toMovement);
    }

    private Page<LedgerMovementDtos.Response> filter(Page<LedgerMovement> page, Instant startDt, Instant endDt,
                                          List<String> statuses) {
        Set<LedgerMovementStatus> statusSet = parseStatuses(statuses);
        List<LedgerMovementDtos.Response> filtered = page.getContent().stream()
            .filter(m -> startDt == null || m.getCreateDt() == null || !m.getCreateDt().isBefore(startDt))
            .filter(m -> endDt == null || m.getCreateDt() == null || !m.getCreateDt().isAfter(endDt))
            .filter(m -> statusSet == null || statusSet.contains(m.getStatus()))
            .map(DtoMapper::toMovement)
            .toList();
        return new PageImpl<>(filtered, page.getPageable(),
            statusSet == null && startDt == null && endDt == null ? page.getTotalElements() : filtered.size());
    }

    private Set<LedgerMovementStatus> parseStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return null;
        return statuses.stream()
            .flatMap(s -> Arrays.stream(s.split(",")))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(LedgerMovementStatus::valueOf)
            .collect(Collectors.toSet());
    }
}
