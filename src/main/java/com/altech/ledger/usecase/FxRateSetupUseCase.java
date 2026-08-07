package com.altech.ledger.usecase;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.FxErrorResponse;
import com.altech.ledger.exception.response.AccountErrorResponse;

import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.FxRateDtos;

@Service
@RequiredArgsConstructor
public class FxRateSetupUseCase {
    private final FxRateRepository fxRates;

    @Transactional
    public FxRateDtos.Response create(FxRateDtos.CreateRequest dto) {
        String base = dto.base().toUpperCase();
        String target = dto.target().toUpperCase();
        if (fxRates.findByBaseAndTarget(base, target).isPresent()) {
            throw new BizException(FxErrorResponse.FX0409, "Rate exists for " + base + "/" + target);
        }
        return DtoMapper.toFx(fxRates.save(new FxRate(base, target, dto.rate())));
    }

    @Transactional
    public FxRateDtos.Response update(Long id, FxRateDtos.CreateRequest dto) {
        FxRate rate = fxRates.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "FxRate not found: " + id));
        rate.setBase(dto.base().toUpperCase());
        rate.setTarget(dto.target().toUpperCase());
        rate.setRate(dto.rate());
        return DtoMapper.toFx(fxRates.save(rate));
    }

    @Transactional(readOnly = true)
    public FxRateDtos.Response getOne(Long id) {
        return DtoMapper.toFx(fxRates.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "FxRate not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<FxRateDtos.Response> getAll(Pageable pageable) {
        return fxRates.findAll(pageable).map(DtoMapper::toFx);
    }
}
