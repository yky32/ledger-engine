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

/**
 * Flat COA profile (no JSONB). Lazy-seed DEFAULT = legacy CoaCodes values.
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

    /** Segments for any member book (settlement or LP) — same entity/type/sub/buffer. */
    @Transactional
    public Segments segments(String profileCode) {
        CoaProfile p = requireByCodeOrDefault(profileCode);
        return new Segments(p.getEntity(), p.getType(), p.getSubType(), p.getBuffer(),
            Boolean.TRUE.equals(p.getPoolAllowNegative()));
    }

    @Transactional
    public boolean poolAllowNegative(String profileCode) {
        return Boolean.TRUE.equals(requireByCodeOrDefault(profileCode).getPoolAllowNegative());
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

    /**
     * Idempotent ensure by code (create if missing). Idempotent ensure by code.
     */
    @Transactional
    public GetCoaProfileResponseDto ensureProfile(
        String code,
        String name,
        String entity,
        boolean isDefault
    ) {
        String c = code.trim().toUpperCase(Locale.ROOT);
        return coaProfileRepository.findByCode(c)
            .map(existing -> {
                // keep existing segments; only refresh name if blank
                if ((existing.getName() == null || existing.getName().isBlank()) && name != null) {
                    existing.setName(name);
                    return toDto(coaProfileRepository.save(existing));
                }
                return toDto(existing);
            })
            .orElseGet(() -> create(new CreateCoaProfileRequestDto(
                c, name, isDefault, true, entity, null, null, null, "LP", true)));
    }

    @Transactional
    public GetCoaProfileResponseDto create(CreateCoaProfileRequestDto req) {
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (coaProfileRepository.existsByCode(code)) {
            throw new BizException(CoaErrorResponse.COA0409, "code=" + code);
        }
        CoaProfile p = new CoaProfile();
        p.setCode(code);
        p.setName(blankTo(req.name(), code));
        p.setIsDefault(Boolean.TRUE.equals(req.isDefault()));
        p.setIsEnabled(req.isEnabled() == null || req.isEnabled());
        p.setEntity(blankTo(req.entity(), CoaCodes.ENTITY));
        p.setType(blankTo(req.type(), CoaCodes.typeCodeLiability()));
        p.setSubType(blankTo(req.subType(), CoaCodes.SUB_TYPE));
        p.setBuffer(blankTo(req.buffer(), CoaCodes.BUFFER));
        p.setLpCurrency(blankTo(req.lpCurrency(), "LP"));
        p.setPoolAllowNegative(req.poolAllowNegative() == null || req.poolAllowNegative());
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
        if (req.entity() != null && !req.entity().isBlank()) {
            p.setEntity(req.entity().trim());
        }
        if (req.type() != null && !req.type().isBlank()) {
            p.setType(req.type().trim());
        }
        if (req.subType() != null && !req.subType().isBlank()) {
            p.setSubType(req.subType().trim());
        }
        if (req.buffer() != null && !req.buffer().isBlank()) {
            p.setBuffer(req.buffer().trim());
        }
        if (req.lpCurrency() != null && !req.lpCurrency().isBlank()) {
            p.setLpCurrency(req.lpCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (req.poolAllowNegative() != null) {
            p.setPoolAllowNegative(req.poolAllowNegative());
        }
        if (req.isDefault() != null) {
            p.setIsDefault(req.isDefault());
            if (Boolean.TRUE.equals(req.isDefault())) {
                _clearOtherDefaults(p.getId());
            }
        }
        return toDto(coaProfileRepository.save(p));
    }

    public String fullNumber(Segments seg, String mainAccount, String subAccount, Currency currency) {
        return CoaCodes.fullNumber(
            seg.entity(), seg.type(), seg.subType(), mainAccount, subAccount, seg.buffer(), currency);
    }

    private CoaProfile _seedDefault() {
        CoaProfile p = new CoaProfile();
        p.setCode("DEFAULT");
        p.setName("Default COA (legacy CoaCodes)");
        p.setIsDefault(true);
        p.setIsEnabled(true);
        p.setEntity(CoaCodes.ENTITY);
        p.setType(CoaCodes.typeCodeLiability());
        p.setSubType(CoaCodes.SUB_TYPE);
        p.setBuffer(CoaCodes.BUFFER);
        p.setLpCurrency("LP");
        p.setPoolAllowNegative(true);
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

    private static String blankTo(String v, String d) {
        return v == null || v.isBlank() ? d : v.trim();
    }

    private GetCoaProfileResponseDto toDto(CoaProfile p) {
        return GetCoaProfileResponseDto.builder()
            .id(p.getId())
            .code(p.getCode())
            .name(p.getName())
            .isDefault(p.getIsDefault())
            .isEnabled(p.getIsEnabled())
            .entity(p.getEntity())
            .type(p.getType())
            .subType(p.getSubType())
            .buffer(p.getBuffer())
            .lpCurrency(p.getLpCurrency())
            .poolAllowNegative(p.getPoolAllowNegative())
            .createDt(p.getCreateDt())
            .updateDt(p.getUpdateDt())
            .build();
    }

    public record Segments(
        String entity,
        String type,
        String subType,
        String buffer,
        boolean poolAllowNegative
    ) {}
}
