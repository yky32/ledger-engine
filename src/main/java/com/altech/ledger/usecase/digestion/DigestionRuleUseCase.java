package com.altech.ledger.usecase.digestion;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateDigestionRuleRequestDto;
import com.altech.ledger.entity.dto.request.UpdateDigestionRuleRequestDto;
import com.altech.ledger.entity.dto.response.GetDigestionRuleResponseDto;
import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.exception.response.DigestionErrorResponse;
import com.altech.ledger.repository.DigestionRuleRepository;
import com.altech.ledger.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DigestionRuleUseCase {
    private final DigestionRuleRepository digestionRuleRepository;

    @Transactional(readOnly = true)
    public GetDigestionRuleResponseDto get(Long id) {
        return DtoWrapper.getDigestionRuleResponseDto(_require(id));
    }

    @Transactional(readOnly = true)
    public GetDigestionRuleResponseDto getByCode(String code) {
        DigestionRule r = digestionRuleRepository.findByCode(code.trim())
            .orElseThrow(() -> new BizException(DigestionErrorResponse.DIG0404, "code=" + code));
        return DtoWrapper.getDigestionRuleResponseDto(r);
    }

    @Transactional(readOnly = true)
    public List<GetDigestionRuleResponseDto> list(Boolean enabledOnly) {
        List<DigestionRule> rows = Boolean.TRUE.equals(enabledOnly)
            ? digestionRuleRepository.findAllEnabledOrdered()
            : digestionRuleRepository.findAllByOrderByPriorityAscIdAsc();
        return rows.stream().map(DtoWrapper::getDigestionRuleResponseDto).toList();
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
        r.setEventType(req.eventType().trim().toUpperCase(Locale.ROOT));
        r.setOperation(req.operation() == null || req.operation().isBlank()
            ? "EARN" : req.operation().trim().toUpperCase(Locale.ROOT));
        r.setIsEnabled(req.isEnabled() == null || req.isEnabled());
        r.setPriority(req.priority() == null ? 100 : req.priority());
        r.setMinAmount(req.minAmount() == null ? BigDecimal.ZERO : req.minAmount());
        r.setEligibleCurrencies(joinCodes(req.eligibleCurrencies()));
        r.setEligibleMccs(joinCodes(req.eligibleMccs()));
        r.setMaxAgeDays(normalizeMaxAgeDays(req.maxAgeDays()));
        r.setResultCurrency(req.resultCurrency() == null || req.resultCurrency().isBlank()
            ? "LP" : req.resultCurrency().trim().toUpperCase(Locale.ROOT));
        try {
            r.setFormula(DigestionFormulaConfig.normalize(req.formula()));
        } catch (IllegalArgumentException ex) {
            throw new BizException(DigestionErrorResponse.DIG0400, ex.getMessage());
        }
        r.setProcessType(req.processType());
        if (req.whenFactors() != null) {
            Object wf = req.whenFactors();
            if (wf instanceof java.util.Collection<?> c && c.isEmpty()) {
                r.setWhenFactors(null);
            } else if (wf instanceof Map<?, ?> m && m.isEmpty()) {
                r.setWhenFactors(null);
            } else {
                r.setWhenFactors(wf);
            }
        }
        r.setIsActive(true);
        return DtoWrapper.getDigestionRuleResponseDto(digestionRuleRepository.save(r));
    }

    @Transactional
    public GetDigestionRuleResponseDto update(Long id, UpdateDigestionRuleRequestDto req) {
        DigestionRule r = _require(id);
        if (req.name() != null) {
            r.setName(req.name());
        }
        if (req.eventType() != null && !req.eventType().isBlank()) {
            r.setEventType(req.eventType().trim().toUpperCase(Locale.ROOT));
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
            r.setEligibleCurrencies(joinCodes(req.eligibleCurrencies()));
        }
        if (req.eligibleMccs() != null) {
            r.setEligibleMccs(joinCodes(req.eligibleMccs()));
        }
        if (req.maxAgeDays() != null) {
            r.setMaxAgeDays(normalizeMaxAgeDays(req.maxAgeDays()));
        }
        if (req.resultCurrency() != null && !req.resultCurrency().isBlank()) {
            r.setResultCurrency(req.resultCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (req.formula() != null) {
            try {
                r.setFormula(DigestionFormulaConfig.normalize(req.formula()));
            } catch (IllegalArgumentException ex) {
                throw new BizException(DigestionErrorResponse.DIG0400, ex.getMessage());
            }
        }
        if (req.processType() != null) {
            r.setProcessType(req.processType().isBlank() ? null : req.processType());
        }
        if (req.whenFactors() != null) {
            Object wf = req.whenFactors();
            if (wf instanceof java.util.Collection<?> c && c.isEmpty()) {
                r.setWhenFactors(null);
            } else if (wf instanceof Map<?, ?> m && m.isEmpty()) {
                r.setWhenFactors(null);
            } else {
                r.setWhenFactors(wf);
            }
        }
        return DtoWrapper.getDigestionRuleResponseDto(digestionRuleRepository.save(r));
    }

    @Transactional
    public GetDigestionRuleResponseDto setEnabled(Long id, boolean enabled) {
        DigestionRule r = _require(id);
        r.setIsEnabled(enabled);
        return DtoWrapper.getDigestionRuleResponseDto(digestionRuleRepository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        DigestionRule r = _require(id);
        digestionRuleRepository.delete(r);
    }

    private DigestionRule _require(Long id) {
        return digestionRuleRepository.findById(id)
            .orElseThrow(() -> new BizException(DigestionErrorResponse.DIG0404, "id=" + id));
    }

    /** 0 or negative → unrestricted (null). Lets all-any upsert clear a stored age gate. */
    static Integer normalizeMaxAgeDays(Integer maxAgeDays) {
        if (maxAgeDays == null || maxAgeDays <= 0) {
            return null;
        }
        return maxAgeDays;
    }

    /** Join currency / MCC codes to CSV (upper). Empty list → null (means unrestricted). */
    public static String joinCodes(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .distinct()
            .collect(Collectors.joining(","));
    }

    public static String joinCurrencies(List<String> list) {
        return joinCodes(list);
    }

    public static List<String> splitCodes(String csv) {
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

    public static List<String> splitCurrencies(String csv) {
        return splitCodes(csv);
    }
}
