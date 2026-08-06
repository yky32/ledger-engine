package com.altech.ledger.usecase;

import com.altech.ledger.entity.dto.parity.ParityDtos.CreateFxRateRequest;
import com.altech.ledger.entity.dto.parity.ParityDtos.FxRateResponse;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FxRateSetupUseCase {
    private final FxRateRepository fxRates;

    @Transactional
    public FxRateResponse create(CreateFxRateRequest dto) {
        String base = dto.base().toUpperCase();
        String target = dto.target().toUpperCase();
        if (fxRates.findByBaseAndTarget(base, target).isPresent()) {
            throw LedgerException.conflict("FX_EXISTS", "Rate exists for " + base + "/" + target);
        }
        return DtoMapper.toFx(fxRates.save(new FxRate(base, target, dto.rate())));
    }

    @Transactional
    public FxRateResponse update(Long id, CreateFxRateRequest dto) {
        FxRate rate = fxRates.findById(id)
            .orElseThrow(() -> LedgerException.notFound("FxRate not found: " + id));
        rate.setBase(dto.base().toUpperCase());
        rate.setTarget(dto.target().toUpperCase());
        rate.setRate(dto.rate());
        return DtoMapper.toFx(fxRates.save(rate));
    }

    @Transactional(readOnly = true)
    public FxRateResponse getOne(Long id) {
        return DtoMapper.toFx(fxRates.findById(id)
            .orElseThrow(() -> LedgerException.notFound("FxRate not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<FxRateResponse> getAll(Pageable pageable) {
        return fxRates.findAll(pageable).map(DtoMapper::toFx);
    }
}
