package com.altech.ledger.usecase.integration;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.request.UpdateIntegrationConfigRequestDto;
import com.altech.ledger.entity.dto.response.GetIntegrationConfigResponseDto;
import com.altech.ledger.entity.po.integration.IntegrationConfig;
import com.altech.ledger.repository.IntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Runtime integration policy from DB (lazy default row). Env properties only seed first row.
 */
@Component
@RequiredArgsConstructor
public class IntegrationConfigUseCase {
    private final IntegrationConfigRepository integrationConfigRepository;
    private final IntegrationProperties integrationProperties;

    /** Effective config for earn path (always non-null). */
    @Transactional
    public IntegrationConfig requireEffective() {
        return integrationConfigRepository.findFirstActive().orElseGet(this::_createDefaultFromEnv);
    }

    @Transactional(readOnly = true)
    public GetIntegrationConfigResponseDto get() {
        return toDto(requireEffectiveReadOnly());
    }

    @Transactional
    public GetIntegrationConfigResponseDto getOrCreate() {
        return toDto(requireEffective());
    }

    @Transactional
    public GetIntegrationConfigResponseDto update(UpdateIntegrationConfigRequestDto req) {
        IntegrationConfig c = requireEffective();
        if (req.isEnabled() != null) {
            c.setIsEnabled(req.isEnabled());
        }
        if (req.isAutoCreateWallet() != null) {
            c.setIsAutoCreateWallet(req.isAutoCreateWallet());
        }
        if (req.autoWalletSettlementCurrency() != null && !req.autoWalletSettlementCurrency().isBlank()) {
            c.setAutoWalletSettlementCurrency(req.autoWalletSettlementCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (req.autoWalletEnsureCurrency() != null && !req.autoWalletEnsureCurrency().isBlank()) {
            c.setAutoWalletEnsureCurrency(req.autoWalletEnsureCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (req.autoWalletAssociatedFrom() != null) {
            c.setAutoWalletAssociatedFrom(req.autoWalletAssociatedFrom().isBlank()
                ? null : req.autoWalletAssociatedFrom().trim());
        }
        if (req.autoWalletNamePrefix() != null) {
            c.setAutoWalletNamePrefix(req.autoWalletNamePrefix());
        }
        return toDto(integrationConfigRepository.save(c));
    }

    private IntegrationConfig requireEffectiveReadOnly() {
        return integrationConfigRepository.findFirstActive()
            .orElseGet(this::_defaultsInMemory);
    }

    private IntegrationConfig _createDefaultFromEnv() {
        IntegrationConfig c = _defaultsInMemory();
        c.setIsActive(true);
        return integrationConfigRepository.save(c);
    }

    private IntegrationConfig _defaultsInMemory() {
        IntegrationConfig c = new IntegrationConfig();
        c.setIsEnabled(integrationProperties.isEnabled());
        c.setIsAutoCreateWallet(integrationProperties.isAutoCreateWallet());
        IntegrationProperties.AutoWallet aw = integrationProperties.getAutoWallet();
        if (aw == null) {
            aw = new IntegrationProperties.AutoWallet();
        }
        c.setAutoWalletSettlementCurrency(
            aw.getSettlementCurrency() == null ? "HKD" : aw.getSettlementCurrency().toUpperCase(Locale.ROOT));
        c.setAutoWalletEnsureCurrency(
            aw.getEnsureCurrency() == null ? "LP" : aw.getEnsureCurrency().toUpperCase(Locale.ROOT));
        c.setAutoWalletAssociatedFrom(aw.getAssociatedFrom() == null ? "CRM" : aw.getAssociatedFrom());
        c.setAutoWalletNamePrefix(aw.getNamePrefix() == null ? "Auto " : aw.getNamePrefix());
        c.setIsActive(true);
        return c;
    }

    private GetIntegrationConfigResponseDto toDto(IntegrationConfig c) {
        return GetIntegrationConfigResponseDto.builder()
            .id(c.getId())
            .isEnabled(c.getIsEnabled())
            .isAutoCreateWallet(c.getIsAutoCreateWallet())
            .autoWalletSettlementCurrency(c.getAutoWalletSettlementCurrency())
            .autoWalletEnsureCurrency(c.getAutoWalletEnsureCurrency())
            .autoWalletAssociatedFrom(c.getAutoWalletAssociatedFrom())
            .autoWalletNamePrefix(c.getAutoWalletNamePrefix())
            .createDt(c.getCreateDt())
            .updateDt(c.getUpdateDt())
            .build();
    }
}
