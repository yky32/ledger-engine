package com.altech.ledger.usecase.config;

import com.altech.ledger.entity.dto.parity.SystemDtos;
import com.altech.ledger.entity.po.configuration.SystemConfiguration;
import com.altech.ledger.repository.SystemConfigurationRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpsertSystemConfigurationUseCase {
    private final SystemConfigurationRepository systemConfigurationRepository;

    @Transactional
    public SystemDtos.ConfigurationResponse execute(String name, String target, String scope, String value) {
        SystemConfiguration cfg = systemConfigurationRepository.findByTargetAndScope(target, scope)
            .orElse(new SystemConfiguration(name, target, scope, value));
        cfg.setName(name);
        cfg.setValue(value);
        return DtoMapper.toConfig(systemConfigurationRepository.save(cfg));
    }
}
