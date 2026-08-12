package com.altech.ledger.usecase.digestion;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateDigestionRuleRequestDto;
import com.altech.ledger.entity.dto.request.UpdateDigestionRuleRequestDto;
import com.altech.ledger.entity.dto.response.GetDigestionRuleResponseDto;
import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.exception.response.DigestionErrorResponse;
import com.altech.ledger.repository.DigestionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DigestionRuleUseCase {
    private final DigestionRuleRepository digestionRuleRepository;

    @Transactional(readOnly = true)
    public GetDigestionRuleResponseDto get(Long id) {
        return toDto(_require(id));
    }

    @Transactional(readOnly = true)
    public GetDigestionRuleResponseDto getByCode(String code) {
        DigestionRule r = digestionRuleRepository.findByCode(code.trim())
            .orElseThrow(() -> new BizException(DigestionErrorResponse.DIG0404, "code=" + code));
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public List<GetDigestionRuleResponseDto> list(Boolean enabledOnly) {
        List<DigestionRule> rows = Boolean.TRUE.equals(enabledOnly)
            ? digestionRuleRepository.findAllEnabledOrdered()
            : digestionRuleRepository.findAllByOrderByPriorityAscIdAsc();
        return rows.stream().map(this::toDto).toList();
    }

    @Transactional
    public GetDigestionRuleResponseDto create(CreateDigestionRuleRequestDto req) {
        String code = req.code().trim();
        if (digestionRuleRepository.existsByCode(code)) {
            throw new BizException(DigestionErrorResponse.DIG0409, "code=" + code);
        }
        DigestionRule r = new DigestionRule();
        r.setCode(code);
        r.setName(req.name());
        r.setEventType(req.eventType().trim());
        r.setOperation(req.operation() == null || req.operation().isBlank()
            ? "EARN" : req.operation().trim().toUpperCase(Locale.ROOT));
        r.setIsEnabled(req.isEnabled() == null || req.isEnabled());
        r.setPriority(req.priority() == null ? 100 : req.priority());
        r.setMinAmount(req.minAmount() == null ? BigDecimal.ZERO : req.minAmount());
        r.setEligibleCurrencies(joinCurrencies(req.eligibleCurrencies()));
        r.setMaxAgeDays(req.maxAgeDays());
        r.setPointCurrency(req.pointCurrency() == null || req.pointCurrency().isBlank()
            ? "LP" : req.pointCurrency().trim().toUpperCase(Locale.ROOT));
        r.setFormula(req.formula().trim());
        r.setProcessType(req.processType());
        r.setIsActive(true);
        return toDto(digestionRuleRepository.save(r));
    }

    @Transactional
    public GetDigestionRuleResponseDto update(Long id, UpdateDigestionRuleRequestDto req) {
        DigestionRule r = _require(id);
        if (req.name() != null) {
            r.setName(req.name());
        }
        if (req.eventType() != null && !req.eventType().isBlank()) {
            r.setEventType(req.eventType().trim());
        }
        if (req.operation() != null && !req.operation().isBlank()) {
            r.setOperation(req.operation().trim().toUpperCase(Locale.ROOT));
        }
        if (req.isEnabled() != null) {
            r.setIsEnabled(req.isEnabled());
        }
        if (req.priority() != null) {
            r.setPriority(req.priority());
        }
        if (req.minAmount() != null) {
            r.setMinAmount(req.minAmount());
        }
        if (req.eligibleCurrencies() != null) {
            r.setEligibleCurrencies(joinCurrencies(req.eligibleCurrencies()));
        }
        if (req.maxAgeDays() != null) {
            r.setMaxAgeDays(req.maxAgeDays()); // use -1 sentinel? allow null via omit only
        }
        // allow clearing maxAgeDays with empty body field — skip for simplicity
        if (req.pointCurrency() != null && !req.pointCurrency().isBlank()) {
            r.setPointCurrency(req.pointCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (req.formula() != null && !req.formula().isBlank()) {
            r.setFormula(req.formula().trim());
        }
        if (req.processType() != null) {
            r.setProcessType(req.processType().isBlank() ? null : req.processType());
        }
        return toDto(digestionRuleRepository.save(r));
    }

    @Transactional
    public GetDigestionRuleResponseDto setEnabled(Long id, boolean enabled) {
        DigestionRule r = _require(id);
        r.setIsEnabled(enabled);
        return toDto(digestionRuleRepository.save(r));
    }

    private DigestionRule _require(Long id) {
        return digestionRuleRepository.findById(id)
            .orElseThrow(() -> new BizException(DigestionErrorResponse.DIG0404, "id=" + id));
    }

    public static String joinCurrencies(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .distinct()
            .collect(Collectors.joining(","));
    }

    public static List<String> splitCurrencies(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String p : csv.split(",")) {
            if (p != null && !p.isBlank()) {
                out.add(p.trim().toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private GetDigestionRuleResponseDto toDto(DigestionRule r) {
        return GetDigestionRuleResponseDto.builder()
            .id(r.getId())
            .code(r.getCode())
            .name(r.getName())
            .eventType(r.getEventType())
            .operation(r.getOperation())
            .isEnabled(r.getIsEnabled())
            .priority(r.getPriority())
            .minAmount(r.getMinAmount())
            .eligibleCurrencies(splitCurrencies(r.getEligibleCurrencies()))
            .maxAgeDays(r.getMaxAgeDays())
            .pointCurrency(r.getPointCurrency())
            .formula(r.getFormula())
            .processType(r.getProcessType())
            .createDt(r.getCreateDt())
            .updateDt(r.getUpdateDt())
            .build();
    }
}
