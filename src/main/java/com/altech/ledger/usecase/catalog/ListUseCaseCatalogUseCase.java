package com.altech.ledger.usecase.catalog;

import com.altech.ledger.entity.dto.response.GetCoaProfileResponseDto;
import com.altech.ledger.entity.dto.response.GetDigestionRuleResponseDto;
import com.altech.ledger.entity.dto.response.UseCaseCatalogItemDto;
import com.altech.ledger.entity.dto.posting.PostingRecipe;
import com.altech.ledger.usecase.coa.CoaProfileUseCase;
import com.altech.ledger.usecase.digestion.DigestionFormulaConfig;
import com.altech.ledger.usecase.digestion.DigestionRuleUseCase;
import com.altech.ledger.usecase.ledger.PostingRecipeCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Merges Brain rules + COA profiles + posting recipes into one catalog for upstream SDK.
 * Ops configures in Admin; xapi discovers via GET /integrations/use-cases then submits by {@code code}.
 */
@Component
@RequiredArgsConstructor
public class ListUseCaseCatalogUseCase {
    private final DigestionRuleUseCase digestionRuleUseCase;
    private final CoaProfileUseCase coaProfileUseCase;
    private final PostingRecipeCatalog postingRecipeCatalog;

    @Transactional(readOnly = true)
    public List<UseCaseCatalogItemDto> list(boolean enabledOnly) {
        Map<String, UseCaseCatalogItemDto> byCode = new LinkedHashMap<>();

        for (GetDigestionRuleResponseDto r : digestionRuleUseCase.list(false)) {
            if (r.getEventType() == null || r.getEventType().isBlank()) {
                continue;
            }
            String code = norm(r.getEventType());
            Object formula = r.getFormula();
            boolean spend = DigestionFormulaConfig.isSpendBased(formula);
            UseCaseCatalogItemDto item = UseCaseCatalogItemDto.builder()
                .code(code)
                .name(blank(r.getName(), code))
                .enabled(Boolean.TRUE.equals(r.getIsEnabled()))
                .operation(r.getOperation())
                .priority(r.getPriority())
                .pointCurrency(r.getPointCurrency() != null ? r.getPointCurrency() : "LP")
                .amountMode(spend ? "SPEND" : "ZERO")
                .formula(formula)
                .formulaSummary(summarizeFormula(formula))
                .hasBrainRule(true)
                .hasCoaProfile(false)
                .hasRecipe(false)
                .build();
            byCode.put(code, item);
        }

        for (GetCoaProfileResponseDto p : coaProfileUseCase.list()) {
            String code = firstNonBlank(p.getTransactionCode(), p.getCode());
            if (code == null) {
                continue;
            }
            code = norm(code);
            if ("DEFAULT".equals(code) && !byCode.containsKey(code)) {
                continue;
            }
            UseCaseCatalogItemDto existing = byCode.get(code);
            if (existing == null) {
                byCode.put(code, UseCaseCatalogItemDto.builder()
                    .code(code)
                    .name(blank(p.getName(), code))
                    .enabled(Boolean.TRUE.equals(p.getIsEnabled()))
                    .amountMode("ANY")
                    .coaProfileCode(p.getCode())
                    .coaCurrency(p.getCurrency())
                    .hasBrainRule(false)
                    .hasCoaProfile(true)
                    .hasRecipe(false)
                    .build());
            } else {
                existing.setCoaProfileCode(p.getCode());
                existing.setCoaCurrency(p.getCurrency());
                existing.setHasCoaProfile(true);
            }
        }

        for (Map.Entry<String, PostingRecipe> e : postingRecipeCatalog.all().entrySet()) {
            String code = norm(e.getKey());
            PostingRecipe recipe = e.getValue();
            UseCaseCatalogItemDto existing = byCode.get(code);
            if (existing == null) {
                byCode.put(code, UseCaseCatalogItemDto.builder()
                    .code(code)
                    .name(code)
                    .enabled(false)
                    .amountMode("ANY")
                    .pointCurrency(recipe.rewardCcy() != null ? recipe.rewardCcy().name() : "LP")
                    .hasBrainRule(false)
                    .hasCoaProfile(false)
                    .hasRecipe(true)
                    .build());
            } else {
                existing.setHasRecipe(true);
                if (existing.getPointCurrency() == null && recipe.rewardCcy() != null) {
                    existing.setPointCurrency(recipe.rewardCcy().name());
                }
            }
        }

        List<UseCaseCatalogItemDto> out = new ArrayList<>(byCode.values());
        if (enabledOnly) {
            out.removeIf(i -> !Boolean.TRUE.equals(i.getEnabled()) || !Boolean.TRUE.equals(i.getHasBrainRule()));
        }
        out.sort(Comparator
            .comparing((UseCaseCatalogItemDto u) -> u.getPriority() == null ? Integer.MAX_VALUE : u.getPriority())
            .thenComparing(u -> u.getCode() == null ? "" : u.getCode()));
        return out;
    }

    @Transactional(readOnly = true)
    public Optional<UseCaseCatalogItemDto> find(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String n = norm(code);
        return list(false).stream().filter(i -> n.equals(i.getCode())).findFirst();
    }

    private static String norm(String s) {
        return s.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static String blank(String v, String d) {
        return v == null || v.isBlank() ? d : v.trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static String summarizeFormula(Object formula) {
        if (formula == null) {
            return null;
        }
        if (formula instanceof Map<?, ?> m) {
            Object t = m.get("type");
            if (t != null && "FIXED".equalsIgnoreCase(String.valueOf(t))) {
                return "FIXED:" + m.get("value");
            }
            if (t != null && "RATE".equalsIgnoreCase(String.valueOf(t))) {
                return "RATE:" + m.get("rate");
            }
            return t == null ? null : String.valueOf(t);
        }
        return String.valueOf(formula);
    }
}
