package com.altech.ledger.usecase.coa;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateCoaProfileRequestDto;
import com.altech.ledger.entity.dto.request.UpdateCoaProfileRequestDto;
import com.altech.ledger.entity.dto.response.GetCoaProfileResponseDto;
import com.altech.ledger.entity.po.coa.CoaProfile;
import com.altech.ledger.exception.response.CoaErrorResponse;
import com.altech.ledger.repository.CoaProfileRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Flat COA profile. Operator binds {@code transactionCode} (eventType) → this profile.
 */
@Component
@RequiredArgsConstructor
public class CoaProfileUseCase {
    private final CoaProfileRepository coaProfileRepository;

    @Transactional
    public CoaProfile requireByCode(String code) {
        if (code == null || code.isBlank() || "DEFAULT".equalsIgnoreCase(code.trim())) {
            throw new BizException(CoaErrorResponse.COA0400, "COA profile code required (no default)");
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

    /**
     * Chart lookup by webhook {@code eventType} only (same token as Door / Brain / accounting).
     * Not used for posting — accounting rules pick books.
     */
    @Transactional
    public CoaProfile resolveForEvent(String eventType, Map<String, String> metadata) {
        if (eventType != null && !eventType.isBlank()) {
            Optional<CoaProfile> hit = findByTransactionCode(eventType);
            if (hit.isPresent()) {
                return hit.get();
            }
        }
        throw new BizException(CoaErrorResponse.COA0404, "no COA for eventType=" + eventType);
    }

    @Transactional(readOnly = true)
    public GetCoaProfileResponseDto getByTransactionCode(String transactionCode) {
        return DtoWrapper.getCoaProfileResponseDto(findByTransactionCode(transactionCode)
            .orElseThrow(() -> new BizException(CoaErrorResponse.COA0404,
                "transactionCode=" + transactionCode)));
    }

    @Transactional
    public Segments segments(String profileCode) {
        if (profileCode == null || profileCode.isBlank()
            || "DEFAULT".equalsIgnoreCase(profileCode.trim())) {
            // Customer CC custodian individual (sheet 01-01-01) — not 10-20-00.
            return new Segments("01", "01", "01", CoaCodes.BUFFER, false);
        }
        CoaProfile p = requireByCode(profileCode);
        return new Segments(p.getEntity(), p.getType(), p.getSubType(), p.getBuffer(),
            Boolean.TRUE.equals(p.getPoolAllowNegative()));
    }

    @Transactional
    public boolean poolAllowNegative(String profileCode) {
        if (profileCode == null || profileCode.isBlank()
            || "DEFAULT".equalsIgnoreCase(profileCode.trim())) {
            return false;
        }
        return Boolean.TRUE.equals(requireByCode(profileCode).getPoolAllowNegative());
    }

    @Transactional
    public List<GetCoaProfileResponseDto> list() {
        _retireDefaultRow();
        return coaProfileRepository.findAllByIsActiveTrueOrderByCodeAsc().stream().map(DtoWrapper::getCoaProfileResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public GetCoaProfileResponseDto get(Long id) {
        return DtoWrapper.getCoaProfileResponseDto(_require(id));
    }

    @Transactional(readOnly = true)
    public GetCoaProfileResponseDto getByCode(String code) {
        CoaProfile p = coaProfileRepository.findByCode(code.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new BizException(CoaErrorResponse.COA0404, "code=" + code));
        return DtoWrapper.getCoaProfileResponseDto(p);
    }

    @Transactional
    public GetCoaProfileResponseDto getOrCreateDefault() {
        _retireDefaultRow();
        throw new BizException(CoaErrorResponse.COA0404, "no default COA");
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
                    return DtoWrapper.getCoaProfileResponseDto(coaProfileRepository.save(existing));
                }
                return DtoWrapper.getCoaProfileResponseDto(existing);
            })
            .orElseGet(() -> create(new CreateCoaProfileRequestDto(
                c, name, null, isDefault, true, entity, null, null, null, "LP", true, null)));
    }

    @Transactional
    public GetCoaProfileResponseDto create(CreateCoaProfileRequestDto req) {
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if ("DEFAULT".equals(code)) {
            throw new BizException(CoaErrorResponse.COA0400, "DEFAULT COA is not used");
        }
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
            .isDefault(false)
            .isEnabled(req.isEnabled() == null || req.isEnabled())
            .entity(blankTo(req.entity(), CoaCodes.ENTITY))
            .type(blankTo(req.type(), CoaCodes.typeCodeLiability()))
            .subType(blankTo(req.subType(), CoaCodes.SUB_TYPE))
            .buffer(blankTo(req.buffer(), CoaCodes.BUFFER))
            .currency(blankTo(req.currency(), "LP"))
            .poolAllowNegative(req.poolAllowNegative() == null || req.poolAllowNegative())
            .walletId(req.walletId())
            .build();
        if (p.getWalletId() == null && CoaCodes.isHouseCode(code)) {
            coaProfileRepository.findAllByIsActiveTrueOrderByCodeAsc().stream()
                .filter(h -> CoaCodes.isHouseCode(h.getCode()) && h.getWalletId() != null)
                .findFirst()
                .ifPresent(h -> p.setWalletId(h.getWalletId()));
        }
        p.setIsActive(true);
        return DtoWrapper.getCoaProfileResponseDto(coaProfileRepository.save(p));
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
        if (req.walletId() != null) {
            p.setWalletId(req.walletId());
        }
        p.setIsDefault(false);
        return DtoWrapper.getCoaProfileResponseDto(coaProfileRepository.save(p));
    }

    public String fullNumber(Segments seg, String mainAccount, Currency currency) {
        return CoaCodes.fullNumber(
            seg.entity(), seg.type(), seg.subType(), mainAccount, seg.buffer(), currency);
    }

    /** Hide legacy DEFAULT chart row — COA has no default profile. */
    private void _retireDefaultRow() {
        coaProfileRepository.findByCode("DEFAULT").ifPresent(p -> {
            boolean dirty = false;
            if (Boolean.TRUE.equals(p.getIsActive())) {
                p.setIsActive(false);
                dirty = true;
            }
            if (Boolean.TRUE.equals(p.getIsEnabled())) {
                p.setIsEnabled(false);
                dirty = true;
            }
            if (Boolean.TRUE.equals(p.getIsDefault())) {
                p.setIsDefault(false);
                dirty = true;
            }
            if (dirty) {
                coaProfileRepository.save(p);
            }
        });
        for (CoaProfile p : coaProfileRepository.findAll()) {
            if (Boolean.TRUE.equals(p.getIsDefault())) {
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

    public record Segments(
        String entity,
        String type,
        String subType,
        String buffer,
        boolean poolAllowNegative
    ) {}
}
