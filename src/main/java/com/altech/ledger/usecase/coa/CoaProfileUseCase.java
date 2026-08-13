package com.altech.ledger.usecase.coa;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateCoaProfileRequestDto;
import com.altech.ledger.entity.dto.request.UpdateCoaProfileRequestDto;
import com.altech.ledger.entity.dto.response.GetCoaProfileResponseDto;
import com.altech.ledger.entity.po.coa.CoaProfile;
import com.altech.ledger.exception.response.CoaErrorResponse;
import com.altech.ledger.repository.CoaProfileRepository;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single-table COA profiles. Lazy-seed DEFAULT (= legacy CoaCodes).
 */
@Component
@RequiredArgsConstructor
public class CoaProfileUseCase {
    private final CoaProfileRepository coaProfileRepository;

    @Transactional
    public CoaProfile requireDefault() {
        List<CoaProfile> defaults = coaProfileRepository.findEnabledDefaults();
        if (!defaults.isEmpty()) {
            return defaults.get(0);
        }
        return _seedDefault();
    }

    @Transactional
    public CoaProfile requireByCodeOrDefault(String code) {
        if (code == null || code.isBlank()) {
            return requireDefault();
        }
        return coaProfileRepository.findByCode(code.trim().toUpperCase(Locale.ROOT))
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && Boolean.TRUE.equals(p.getIsEnabled()))
            .orElseThrow(() -> new BizException(CoaErrorResponse.COA0404, "code=" + code));
    }

    @Transactional
    public Map<String, Object> effectiveBindings(String profileCode) {
        return requireByCodeOrDefault(profileCode).bindingsMap();
    }

    @Transactional
    public CoaBindings.RoleSegments segmentsForMemberCurrency(String profileCode, Currency currency) {
        return CoaBindings.forMemberCurrency(effectiveBindings(profileCode), currency);
    }

    @Transactional
    public CoaBindings.RoleSegments segmentsForPool(String profileCode) {
        return CoaBindings.require(effectiveBindings(profileCode), CoaBindings.ROLE_PROGRAM_POOL);
    }

    @Transactional(readOnly = true)
    public List<GetCoaProfileResponseDto> list() {
        requireDefault();
        return coaProfileRepository.findAllByIsActiveTrueOrderByCodeAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GetCoaProfileResponseDto get(Long id) {
        return toDto(_require(id));
    }

    @Transactional(readOnly = true)
    public GetCoaProfileResponseDto getByCode(String code) {
        CoaProfile p = coaProfileRepository.findByCode(code.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new BizException(CoaErrorResponse.COA0404, "code=" + code));
        return toDto(p);
    }

    @Transactional
    public GetCoaProfileResponseDto getOrCreateDefault() {
        return toDto(requireDefault());
    }

    @Transactional
    public GetCoaProfileResponseDto create(CreateCoaProfileRequestDto req) {
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (coaProfileRepository.existsByCode(code)) {
            throw new BizException(CoaErrorResponse.COA0409, "code=" + code);
        }
        CoaProfile p = new CoaProfile();
        p.setCode(code);
        p.setName(req.name() == null || req.name().isBlank() ? code : req.name().trim());
        p.setIsDefault(Boolean.TRUE.equals(req.isDefault()));
        p.setIsEnabled(req.isEnabled() == null || req.isEnabled());
        try {
            p.setBindingsMap(CoaBindings.normalize(req.bindings()));
        } catch (RuntimeException ex) {
            throw new BizException(CoaErrorResponse.COA0400, ex.getMessage());
        }
        p.setIsActive(true);
        if (Boolean.TRUE.equals(p.getIsDefault())) {
            _clearOtherDefaults(null);
        }
        return toDto(coaProfileRepository.save(p));
    }

    @Transactional
    public GetCoaProfileResponseDto update(Long id, UpdateCoaProfileRequestDto req) {
        CoaProfile p = _require(id);
        if (req.name() != null) {
            p.setName(req.name().isBlank() ? p.getCode() : req.name().trim());
        }
        if (req.isEnabled() != null) {
            p.setIsEnabled(req.isEnabled());
        }
        if (req.bindings() != null) {
            try {
                p.setBindingsMap(CoaBindings.normalize(req.bindings()));
            } catch (RuntimeException ex) {
                throw new BizException(CoaErrorResponse.COA0400, ex.getMessage());
            }
        }
        if (req.isDefault() != null) {
            p.setIsDefault(req.isDefault());
            if (Boolean.TRUE.equals(req.isDefault())) {
                _clearOtherDefaults(p.getId());
            }
        }
        return toDto(coaProfileRepository.save(p));
    }

    /** Compose fullNumber using profile segments + main/sub + ccy. */
    public String fullNumber(CoaBindings.RoleSegments seg, String mainAccount, String subAccount, Currency currency) {
        return CoaCodes.fullNumber(
            seg.entity(), seg.type(), seg.subType(), mainAccount, subAccount, seg.buffer(), currency);
    }

    private CoaProfile _seedDefault() {
        CoaProfile p = new CoaProfile();
        p.setCode("DEFAULT");
        p.setName("Default COA (legacy CoaCodes)");
        p.setIsDefault(true);
        p.setIsEnabled(true);
        p.setBindingsMap(CoaBindings.defaultBindings());
        p.setIsActive(true);
        return coaProfileRepository.save(p);
    }

    private void _clearOtherDefaults(Long keepId) {
        for (CoaProfile p : coaProfileRepository.findAll()) {
            if (Boolean.TRUE.equals(p.getIsDefault()) && (keepId == null || !keepId.equals(p.getId()))) {
                p.setIsDefault(false);
                coaProfileRepository.save(p);
            }
        }
    }

    private CoaProfile _require(Long id) {
        return coaProfileRepository.findById(id)
            .orElseThrow(() -> new BizException(CoaErrorResponse.COA0404, "id=" + id));
    }

    private GetCoaProfileResponseDto toDto(CoaProfile p) {
        return GetCoaProfileResponseDto.builder()
            .id(p.getId())
            .code(p.getCode())
            .name(p.getName())
            .isDefault(p.getIsDefault())
            .isEnabled(p.getIsEnabled())
            .bindings(p.bindingsMap())
            .createDt(p.getCreateDt())
            .updateDt(p.getUpdateDt())
            .build();
    }
}
