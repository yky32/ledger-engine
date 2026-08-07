package com.altech.ledger.usecase.fx;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.FxRateRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import com.altech.ledger.entity.dto.response.GetFxRateResponseDto;

@Component
@RequiredArgsConstructor
public class QueryFxRateUseCase {
    private final FxRateRepository fxRateRepository;
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public GetFxRateResponseDto one(Long id) {
        return DtoMapper.toFx(fxRateRepository.findById(id)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "FxRate not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<GetFxRateResponseDto> list(Pageable pageable, String base, String target) {
        if (base != null && target != null) {
            String b = commonUseCase.normalizeCurrency(base);
            String t = commonUseCase.normalizeCurrency(target);
            return fxRateRepository.findByBaseAndTarget(b, t)
                .<Page<GetFxRateResponseDto>>map(r -> new PageImpl<>(List.of(DtoMapper.toFx(r)), pageable, 1))
                .orElseGet(() -> Page.empty(pageable));
        }
        return fxRateRepository.findAll(pageable).map(DtoMapper::toFx);
    }

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (from == null || to == null || from.equalsIgnoreCase(to)) {
            return amount;
        }
        Optional<FxRate> direct = fxRateRepository.findByBaseAndTarget(
            commonUseCase.normalizeCurrency(from), commonUseCase.normalizeCurrency(to));
        if (direct.isPresent()) {
            return amount.multiply(direct.get().getRate()).setScale(8, RoundingMode.HALF_UP);
        }
        Optional<FxRate> inverse = fxRateRepository.findByBaseAndTarget(
            commonUseCase.normalizeCurrency(to), commonUseCase.normalizeCurrency(from));
        if (inverse.isPresent() && inverse.get().getRate().signum() != 0) {
            return amount.divide(inverse.get().getRate(), 8, RoundingMode.HALF_UP);
        }
        return amount;
    }
}
