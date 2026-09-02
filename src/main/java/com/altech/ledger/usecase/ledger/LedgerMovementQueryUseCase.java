package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.util.Pageables;
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
 * Pageable is 1-based at API boundary; converted via {@link Pageables#toZeroBased}.
 */
@Component
@RequiredArgsConstructor
public class LedgerMovementQueryUseCase {
    private final LedgerMovementRepository ledgerMovementRepository;
    private final WalletRepository walletRepository;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public GetLedgerMovementResponseDto one(Long id) {
        return DtoWrapper.getLedgerMovementResponseDto(commonUseCase.requireMovement(id));
    }

    @Transactional(readOnly = true)
    public Page<GetLedgerMovementResponseDto> list(Pageable pageable, String startDt, String endDt,
                                                  List<String> statuses) {
        Pageable zero = Pageables.toZeroBased(pageable);
        Instant from = Pageables.parseStartDt(startDt);
        Instant to = Pageables.parseEndDt(endDt);
        return _filter(ledgerMovementRepository.findAll(zero), from, to, statuses);
    }

    @Transactional(readOnly = true)
    public Page<GetLedgerMovementResponseDto> myMovements(String ownerId, Pageable pageable,
                                                         String startDt, String endDt, List<String> statuses) {
        Pageable zero = Pageables.toZeroBased(pageable);
        Instant from = Pageables.parseStartDt(startDt);
        Instant to = Pageables.parseEndDt(endDt);
        List<Long> ids = walletRepository.findAllByOwnerId(ownerId).stream().map(Wallet::getId).toList();
        if (ids.isEmpty()) {
            return Page.empty(zero);
        }
        return _filter(ledgerMovementRepository.findByWalletIdIn(ids, zero), from, to, statuses);
    }

    @Transactional(readOnly = true)
    public Page<GetLedgerMovementResponseDto> byWallet(Long walletId, Pageable pageable) {
        return ledgerMovementRepository.findByWalletId(walletId, Pageables.toZeroBased(pageable))
            .map(DtoWrapper::getLedgerMovementResponseDto);
    }

    private Page<GetLedgerMovementResponseDto> _filter(Page<LedgerMovement> page, Instant startDt, Instant endDt,
                                                      List<String> statuses) {
        Set<LedgerMovementStatus> statusSet = _parseStatuses(statuses);
        List<GetLedgerMovementResponseDto> filtered = page.getContent().stream()
            .filter(m -> m.getCreateDt() == null || !m.getCreateDt().isBefore(startDt))
            .filter(m -> m.getCreateDt() == null || !m.getCreateDt().isAfter(endDt))
            .filter(m -> statusSet == null || statusSet.contains(m.getStatus()))
            .map(DtoWrapper::getLedgerMovementResponseDto)
            .toList();
        boolean noFilter = statusSet == null
            && startDt.equals(Pageables.EARLIEST)
            && endDt.equals(Pageables.FAR_FUTURE);
        return new PageImpl<>(filtered, page.getPageable(),
            noFilter ? page.getTotalElements() : filtered.size());
    }

    private Set<LedgerMovementStatus> _parseStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return statuses.stream()
            .flatMap(s -> Arrays.stream(s.split(",")))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> LedgerMovementStatus.valueOf(s.toUpperCase()))
            .collect(Collectors.toSet());
    }
}
