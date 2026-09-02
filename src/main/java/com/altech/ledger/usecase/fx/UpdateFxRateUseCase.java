package com.altech.ledger.usecase.fx;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.request.CreateFxRateRequestDto;
import com.altech.ledger.entity.dto.response.GetFxRateResponseDto;

@Component
@RequiredArgsConstructor
public class UpdateFxRateUseCase {
    private final FxRateRepository fxRateRepository;
    private final CommonUseCase commonUseCase;

    @Transactional
    public GetFxRateResponseDto execute(Long id, CreateFxRateRequestDto dto) {
        FxRate rate = fxRateRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "FxRate not found: " + id));
        rate.setBase(commonUseCase.requireCurrency(dto.base()));
        rate.setTarget(commonUseCase.requireCurrency(dto.target()));
        rate.setRate(dto.rate());
        return DtoWrapper.getFxRateResponseDto(fxRateRepository.save(rate));
    }
}
