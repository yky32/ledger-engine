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
import java.util.Optional;

/**
 * Flat COA profile. Operator binds {@code transactionCode} (eventType) → this profile.
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

    /**
     * Resolve COA by business eventType / transaction code.
     * Looks up {@code transactionCode}, then falls back to {@code code} (same-field assumption).
     */
    @Transactional(readOnly = true)
    public Optional<CoaProfile> findByTransactionCode(String transactionCode) {
        if (transactionCode == null || transactionCode.isBlank()) {
            return Optional.empty();
        }
        String t = _normTxn(transactionCode);
        if (t == null) {
            return Optional.empty();
        }
        Optional<CoaProfile> byTxn = coaProfileRepository.findByTransactionCode(t)
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && Boolean.TRUE.equals(p.getIsEnabled()));
        if (byTxn.isPresent()) {
            return byTxn;
        }
        return coaProfileRepository.findByCode(t)
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && Boolean.TRUE.equals(p.getIsEnabled()));
    }

    @Transactional(readOnly = true)
    public GetCoaProfileResponseDto getByTransactionCode(String transactionCode) {
        return toDto(findByTransactionCode(transactionCode)
            .orElseThrow(() -> new BizException(CoaErrorResponse.COA0404,
                "transactionCode=" + transactionCode)));
    }

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
                if ((existing.getName() == null || existing.getName().isBlank()) && name != null) {
                    existing.setName(name);
                    return toDto(coaProfileRepository.save(existing));
                }
                return toDto(existing);
            })
            .orElseGet(() -> create(new CreateCoaProfileRequestDto(
                c, name, null, isDefault, true, entity, null, null, null, "LP", true)));
    }

    @Transactional
    public GetCoaProfileResponseDto create(CreateCoaProfileRequestDto req) {
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (coaProfileRepository.existsByCode(code)) {
            throw new BizException(CoaErrorResponse.COA0409, "code=" + code);
        }
        // Default: transactionCode == code (unless operator overrides)
        String txn = _normTxn(req.transactionCode());
        if (txn == null) {
            txn = code;
        }
        if (coaProfileRepository.existsByTransactionCode(txn)) {
            throw new BizException(CoaErrorResponse.COA0409, "transactionCode=" + txn);
        }
        CoaProfile p = CoaProfile.builder()
            .code(code)
            .name(blankTo(req.name(), code))
            .transactionCode(txn)
            .isDefault(Boolean.TRUE.equals(req.isDefault()))
            .isEnabled(req.isEnabled() == null || req.isEnabled())
            .entity(blankTo(req.entity(), CoaCodes.ENTITY))
            .type(blankTo(req.type(), CoaCodes.typeCodeLiability()))
            .subType(blankTo(req.subType(), CoaCodes.SUB_TYPE))
            .buffer(blankTo(req.buffer(), CoaCodes.BUFFER))
            .currency(blankTo(req.currency(), "LP"))
            .poolAllowNegative(req.poolAllowNegative() == null || req.poolAllowNegative())
            .build();
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
        if (req.transactionCode() != null) {
            // empty string → fall back to same as code (default assumption)
            String txn = _normTxn(req.transactionCode());
            if (txn == null) {
                txn = p.getCode();
            }
            final String txnFinal = txn;
            coaProfileRepository.findByTransactionCode(txnFinal).ifPresent(other -> {
                if (!other.getId().equals(p.getId())) {
                    throw new BizException(CoaErrorResponse.COA0409, "transactionCode=" + txnFinal);
                }
            });
            p.setTransactionCode(txnFinal);
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
        if (req.currency() != null && !req.currency().isBlank()) {
            p.setCurrency(req.currency().trim().toUpperCase(Locale.ROOT));
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
        CoaProfile p = CoaProfile.builder()
            .code("DEFAULT")
            .name("Default COA (legacy CoaCodes)")
            .isDefault(true)
            .isEnabled(true)
            .entity(CoaCodes.ENTITY)
            .type(CoaCodes.typeCodeLiability())
            .subType(CoaCodes.SUB_TYPE)
            .buffer(CoaCodes.BUFFER)
            .currency("LP")
            .poolAllowNegative(true)
            .build();
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

    private static String _normTxn(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return t.isEmpty() ? null : t;
    }

    private GetCoaProfileResponseDto toDto(CoaProfile p) {
        return GetCoaProfileResponseDto.builder()
            .id(p.getId())
            .code(p.getCode())
            .name(p.getName())
            .transactionCode(p.getTransactionCode())
            .isDefault(p.getIsDefault())
            .isEnabled(p.getIsEnabled())
            .entity(p.getEntity())
            .type(p.getType())
            .subType(p.getSubType())
            .buffer(p.getBuffer())
            .currency(p.getCurrency())
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
