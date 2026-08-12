package com.altech.ledger.usecase.fx;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateFxRateRequestDto;
import com.altech.ledger.entity.dto.response.GetFxRateResponseDto;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.exception.response.FxErrorResponse;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateFxRateUseCase {
    private final FxRateRepository fxRateRepository;
    private final CommonUseCase commonUseCase;

    @Transactional
    public GetFxRateResponseDto execute(CreateFxRateRequestDto dto) {
        Currency base = commonUseCase.requireCurrency(dto.base());
        Currency target = commonUseCase.requireCurrency(dto.target());
        if (fxRateRepository.findByBaseAndTarget(base, target).isPresent()) {
            throw new BizException(FxErrorResponse.FX0409, "Rate exists for " + base + "/" + target);
        }
        FxRate fx = new FxRate();
        fx.setBase(base);
        fx.setTarget(target);
        fx.setRate(dto.rate());
        return DtoMapper.toFx(fxRateRepository.save(fx));
    }
}
