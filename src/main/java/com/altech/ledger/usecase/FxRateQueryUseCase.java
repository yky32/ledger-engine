package com.altech.ledger.usecase;

import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.FxRateDtos;

/**
 * Port of the-wallet-ledger FxRateQueryUseCase.
 */
@Service
@RequiredArgsConstructor
public class FxRateQueryUseCase {
    private final FxRateRepository fxRates;

    @Transactional(readOnly = true)
    public FxRateDtos.Response getOne(Long id) {
        return DtoMapper.toFx(fxRates.findById(id)
            .orElseThrow(() -> LedgerException.notFound("FxRate not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<FxRateDtos.Response> getAll(Pageable pageable, String base, String target) {
        if (base != null && target != null) {
            return fxRates.findByBaseAndTarget(base.toUpperCase(), target.toUpperCase())
                .<Page<FxRateDtos.Response>>map(r -> new org.springframework.data.domain.PageImpl<>(
                    java.util.List.of(DtoMapper.toFx(r)), pageable, 1))
                .orElseGet(() -> Page.empty(pageable));
        }
        return fxRates.findAll(pageable).map(DtoMapper::toFx);
    }

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null) return BigDecimal.ZERO;
        if (from == null || to == null || from.equalsIgnoreCase(to)) return amount;
        Optional<FxRate> direct = fxRates.findByBaseAndTarget(from.toUpperCase(), to.toUpperCase());
        if (direct.isPresent()) {
            return amount.multiply(direct.get().getRate()).setScale(8, RoundingMode.HALF_UP);
        }
        Optional<FxRate> inverse = fxRates.findByBaseAndTarget(to.toUpperCase(), from.toUpperCase());
        if (inverse.isPresent() && inverse.get().getRate().signum() != 0) {
            return amount.divide(inverse.get().getRate(), 8, RoundingMode.HALF_UP);
        }
        return amount;
    }
}
