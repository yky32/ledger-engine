package com.altech.ledger.usecase.config;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.parity.SystemDtos;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.SystemConfigurationRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class QuerySystemConfigurationUseCase {
    private final SystemConfigurationRepository configs;

    @Transactional(readOnly = true)
    public SystemDtos.ConfigurationResponse execute(String target, String scope) {
        return configs.findByTargetAndScope(target, scope)
            .map(DtoMapper::toConfig)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Config not found: " + target + "/" + scope));
    }
}
