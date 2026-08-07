package com.altech.ledger.usecase.config;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.SystemConfigurationRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.response.GetSystemConfigurationResponseDto;

@Component
@RequiredArgsConstructor
public class QuerySystemConfigurationUseCase {
    private final SystemConfigurationRepository systemConfigurationRepository;

    @Transactional(readOnly = true)
    public GetSystemConfigurationResponseDto execute(String target, String scope) {
        return systemConfigurationRepository.findByTargetAndScope(target, scope)
            .map(DtoMapper::toConfig)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Config not found: " + target + "/" + scope));
    }
}
