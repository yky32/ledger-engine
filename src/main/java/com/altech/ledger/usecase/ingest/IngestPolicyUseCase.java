package com.altech.ledger.usecase.ingest;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.request.UpdateIngestPolicyRequestDto;
import com.altech.ledger.entity.dto.response.GetIngestPolicyResponseDto;
import com.altech.ledger.entity.po.ingest.IngestPolicy;
import com.altech.ledger.repository.IngestPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Runtime ingest policy from DB (lazy default row). Env properties only seed first row.
 */
@Component
@RequiredArgsConstructor
public class IngestPolicyUseCase {
    private final IngestPolicyRepository ingestPolicyRepository;
    private final IntegrationProperties integrationProperties;

    @Transactional
    public IngestPolicy requireEffective() {
        return ingestPolicyRepository.findFirstActive().orElseGet(this::_createDefaultFromEnv);
    }

    @Transactional
    public GetIngestPolicyResponseDto getOrCreate() {
        return toDto(requireEffective());
    }

    @Transactional
    public GetIngestPolicyResponseDto update(UpdateIngestPolicyRequestDto req) {
        IngestPolicy p = requireEffective();
        if (req.isEnabled() != null) {
            p.setIsEnabled(req.isEnabled());
        }
        if (req.isAutoCreateWallet() != null) {
            p.setIsAutoCreateWallet(req.isAutoCreateWallet());
        }
        if (req.autoWalletSettlementCurrency() != null && !req.autoWalletSettlementCurrency().isBlank()) {
            p.setAutoWalletSettlementCurrency(req.autoWalletSettlementCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (req.autoWalletEnsureCurrency() != null && !req.autoWalletEnsureCurrency().isBlank()) {
            p.setAutoWalletEnsureCurrency(req.autoWalletEnsureCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (req.autoWalletAssociatedFrom() != null) {
            p.setAutoWalletAssociatedFrom(req.autoWalletAssociatedFrom().isBlank()
                ? null : req.autoWalletAssociatedFrom().trim());
        }
        if (req.autoWalletNamePrefix() != null) {
            p.setAutoWalletNamePrefix(req.autoWalletNamePrefix());
        }
        if (req.autoWalletCoaProfileCode() != null) {
            String code = req.autoWalletCoaProfileCode().trim();
            p.setAutoWalletCoaProfileCode(code.isEmpty() ? null : code.toUpperCase(Locale.ROOT));
        }
        if (req.entryFactors() != null) {
            p.setEntryFactors(req.entryFactors().isEmpty() ? null : List.copyOf(req.entryFactors()));
        }
        return toDto(ingestPolicyRepository.save(p));
    }

    private IngestPolicy _createDefaultFromEnv() {
        IngestPolicy p = _defaultsInMemory();
        p.setIsActive(true);
        return ingestPolicyRepository.save(p);
    }

    private IngestPolicy _defaultsInMemory() {
        IngestPolicy p = new IngestPolicy();
        p.setIsEnabled(integrationProperties.isEnabled());
        p.setIsAutoCreateWallet(integrationProperties.isAutoCreateWallet());
        IntegrationProperties.AutoWallet aw = integrationProperties.getAutoWallet();
        if (aw == null) {
            aw = new IntegrationProperties.AutoWallet();
        }
        p.setAutoWalletSettlementCurrency(
            aw.getSettlementCurrency() == null ? "HKD" : aw.getSettlementCurrency().toUpperCase(Locale.ROOT));
        p.setAutoWalletEnsureCurrency(
            aw.getEnsureCurrency() == null ? "LP" : aw.getEnsureCurrency().toUpperCase(Locale.ROOT));
        p.setAutoWalletAssociatedFrom(aw.getAssociatedFrom() == null ? "CRM" : aw.getAssociatedFrom());
        p.setAutoWalletNamePrefix(aw.getNamePrefix() == null ? "Auto " : aw.getNamePrefix());
        p.setIsActive(true);
        return p;
    }

    private GetIngestPolicyResponseDto toDto(IngestPolicy p) {
        return GetIngestPolicyResponseDto.builder()
            .id(p.getId())
            .isEnabled(p.getIsEnabled())
            .isAutoCreateWallet(p.getIsAutoCreateWallet())
            .autoWalletSettlementCurrency(p.getAutoWalletSettlementCurrency())
            .autoWalletEnsureCurrency(p.getAutoWalletEnsureCurrency())
            .autoWalletAssociatedFrom(p.getAutoWalletAssociatedFrom())
            .autoWalletNamePrefix(p.getAutoWalletNamePrefix())
            .autoWalletCoaProfileCode(p.getAutoWalletCoaProfileCode())
            .entryFactors(p.getEntryFactors())
            .createDt(p.getCreateDt())
            .updateDt(p.getUpdateDt())
            .build();
    }
}
