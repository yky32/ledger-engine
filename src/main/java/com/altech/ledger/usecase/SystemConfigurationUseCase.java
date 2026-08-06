package com.altech.ledger.usecase;

import com.altech.ledger.entity.dto.parity.ParityDtos.ConfigurationResponse;
import com.altech.ledger.entity.po.configuration.SystemConfiguration;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.SystemConfigurationRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemConfigurationUseCase {
    private final SystemConfigurationRepository configs;

    @Transactional(readOnly = true)
    public ConfigurationResponse myConfigurations(String target, String scope) {
        return configs.findByTargetAndScope(target, scope)
            .map(DtoMapper::toConfig)
            .orElseThrow(() -> LedgerException.notFound("Config not found: " + target + "/" + scope));
    }

    @Transactional
    public ConfigurationResponse upsert(String name, String target, String scope, String value) {
        SystemConfiguration cfg = configs.findByTargetAndScope(target, scope)
            .orElse(new SystemConfiguration(name, target, scope, value));
        cfg.setName(name);
        cfg.setValue(value);
        return DtoMapper.toConfig(configs.save(cfg));
    }
}
