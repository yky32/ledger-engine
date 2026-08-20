package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Catalog row for upstream SDK — what ops configured that xapi may invoke.
 * {@code code} is the value to send as {@code eventType} (and COA transactionCode when present).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UseCaseCatalogItemDto {
    /** Stable invoke key = eventType (e.g. LIKE_FB_PAGE, CC_TXN_LP). */
    private String code;
    private String name;
    private Boolean enabled;
    private String operation;
    private Integer priority;
    private String pointCurrency;
    /** ZERO | SPEND | ANY */
    private String amountMode;
    private Object formula;
    private String formulaSummary;
    private String coaProfileCode;
    private String coaCurrency;
    private Boolean hasBrainRule;
    private Boolean hasCoaProfile;
    private Boolean hasRecipe;
}
