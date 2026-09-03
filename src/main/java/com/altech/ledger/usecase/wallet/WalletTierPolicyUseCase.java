package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.UpdateWalletTierPolicyRequestDto;
import com.altech.ledger.entity.dto.response.GetWalletTierPolicyResponseDto;
import com.altech.ledger.entity.enu.WalletTierCriterion;
import com.altech.ledger.entity.json_context.WalletTierBand;
import com.altech.ledger.entity.po.wallet.WalletTierPolicy;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.WalletTierPolicyRepository;
import com.altech.ledger.service.DtoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Runtime wallet-tier policy from DB (lazy default row), Door-shaped.
 */
@Component
@RequiredArgsConstructor
public class WalletTierPolicyUseCase {
    private final WalletTierPolicyRepository walletTierPolicyRepository;

    @Transactional
    public WalletTierPolicy requireEffective() {
        return walletTierPolicyRepository.findFirstActive().orElseGet(this::_createDefault);
    }

    @Transactional
    public GetWalletTierPolicyResponseDto getOrCreate() {
        return DtoWrapper.getWalletTierPolicyResponseDto(requireEffective());
    }

    @Transactional
    public GetWalletTierPolicyResponseDto update(UpdateWalletTierPolicyRequestDto req) {
        WalletTierPolicy p = requireEffective();
        if (req.isEnabled() != null) {
            p.setIsEnabled(req.isEnabled());
        }
        if (req.criterion() != null && !req.criterion().isBlank()) {
            try {
                p.setCriterion(WalletTierCriterion.from(req.criterion()));
            } catch (IllegalArgumentException ex) {
                throw new BizException(WalletErrorResponse.WAL0400, ex.getMessage());
            }
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
        if (req.currency() != null && !req.currency().isBlank()) {
            String iso = req.currency().trim().toUpperCase(Locale.ROOT);
            try {
                Currency.get(iso);
            } catch (RuntimeException ex) {
                throw new BizException(WalletErrorResponse.WAL0400, "unknown currency " + iso);
            }
            p.setCurrency(iso);
        }
        if (req.bands() != null) {
            p.setBands(validateBands(req.bands()));
        }
        return DtoWrapper.getWalletTierPolicyResponseDto(walletTierPolicyRepository.save(p));
    }

    public static List<WalletTierBand> ordered(List<WalletTierBand> bands) {
        if (bands == null || bands.isEmpty()) {
            return List.of();
        }
        List<WalletTierBand> copy = new ArrayList<>(bands);
        copy.sort(Comparator.comparing(b -> nz(b.upgradeAt())));
        return copy;
    }

    public static List<WalletTierBand> validateBands(List<WalletTierBand> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new BizException(WalletErrorResponse.WAL0400, "bands required");
        }
        List<WalletTierBand> cleaned = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (WalletTierBand b : raw) {
            if (b == null || b.code() == null) {
                throw new BizException(WalletErrorResponse.WAL0400, "band.code required");
            }
            if (b.upgradeAt() == null) {
                throw new BizException(WalletErrorResponse.WAL0400,
                    "band.upgradeAt required for " + b.code());
            }
            if (b.upgradeAt().signum() < 0) {
                throw new BizException(WalletErrorResponse.WAL0400,
                    "band.upgradeAt must be >= 0 for " + b.code());
            }
            if (!codes.add(b.code())) {
                throw new BizException(WalletErrorResponse.WAL0400, "duplicate band.code " + b.code());
            }
            cleaned.add(b);
        }
        List<WalletTierBand> ordered = ordered(cleaned);
        for (int i = 1; i < ordered.size(); i++) {
            WalletTierBand prev = ordered.get(i - 1);
            WalletTierBand cur = ordered.get(i);
            if (cur.upgradeAt().compareTo(prev.upgradeAt()) <= 0) {
                throw new BizException(WalletErrorResponse.WAL0400,
                    "band.upgradeAt must strictly increase: " + prev.code() + " → " + cur.code());
            }
            if (cur.downgradeBelow() != null) {
                if (cur.downgradeBelow().compareTo(cur.upgradeAt()) >= 0) {
                    throw new BizException(WalletErrorResponse.WAL0400,
                        "downgradeBelow must be < upgradeAt for " + cur.code());
                }
                if (cur.downgradeBelow().compareTo(prev.upgradeAt()) < 0) {
                    throw new BizException(WalletErrorResponse.WAL0400,
                        "downgradeBelow must be >= previous upgradeAt for " + cur.code());
                }
            }
        }
        return ordered;
    }

    /**
     * Next band for {@code lp} given {@code current} code. Hysteresis: upgrade uses
     * {@code upgradeAt}; downgrade only when {@code lp < current.downgradeBelow}.
     */
    public static String nextTier(List<WalletTierBand> bands, String current, BigDecimal lp) {
        List<WalletTierBand> ordered = ordered(bands);
        if (ordered.isEmpty()) {
            return current;
        }
        BigDecimal amount = lp == null ? BigDecimal.ZERO : lp;
        int curIdx = indexOf(ordered, current);
        int upIdx = 0;
        for (int i = 0; i < ordered.size(); i++) {
            if (amount.compareTo(nz(ordered.get(i).upgradeAt())) >= 0) {
                upIdx = i;
            }
        }
        if (upIdx > curIdx) {
            return ordered.get(upIdx).code();
        }
        WalletTierBand cur = ordered.get(curIdx);
        BigDecimal floor = cur.downgradeBelow() != null ? cur.downgradeBelow() : nz(cur.upgradeAt());
        if (curIdx > 0 && amount.compareTo(floor) < 0) {
            int downIdx = 0;
            for (int i = 0; i < ordered.size(); i++) {
                if (amount.compareTo(nz(ordered.get(i).upgradeAt())) >= 0) {
                    downIdx = i;
                }
            }
            return ordered.get(downIdx).code();
        }
        return cur.code();
    }

    private static int indexOf(List<WalletTierBand> ordered, String current) {
        if (current == null || current.isBlank()) {
            return 0;
        }
        String c = current.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < ordered.size(); i++) {
            if (c.equals(ordered.get(i).code())) {
                return i;
            }
        }
        return 0;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private WalletTierPolicy _createDefault() {
        WalletTierPolicy p = new WalletTierPolicy();
        p.setIsActive(true);
        p.setIsEnabled(true);
        p.setCriterion(WalletTierCriterion.LEDGER_BALANCE);
        p.setEntity("01");
        p.setType("01");
        p.setSubType("01");
        p.setCurrency("LP");
        p.setBands(List.of(
            new WalletTierBand("NONE", BigDecimal.ZERO, null),
            new WalletTierBand("SILVER", new BigDecimal("1000"), null),
            new WalletTierBand("GOLD", new BigDecimal("10000"), new BigDecimal("800")),
            new WalletTierBand("PLATINUM", new BigDecimal("50000"), new BigDecimal("8000"))
        ));
        return walletTierPolicyRepository.save(p);
    }
}
