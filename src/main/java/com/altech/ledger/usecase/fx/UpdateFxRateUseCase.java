package com.altech.ledger.usecase.fx;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.parity.FxRateDtos;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateFxRateUseCase {
    private final FxRateRepository fxRates;
    private final CommonUseCase commonUseCase;

    @Transactional
    public FxRateDtos.Response execute(Long id, FxRateDtos.CreateRequest dto) {
        FxRate rate = fxRates.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "FxRate not found: " + id));
        rate.setBase(commonUseCase.normalizeCurrency(dto.base()));
        rate.setTarget(commonUseCase.normalizeCurrency(dto.target()));
        rate.setRate(dto.rate());
        return DtoMapper.toFx(fxRates.save(rate));
    }
}
