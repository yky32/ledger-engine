package com.altech.ledger.util;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;

/**
 * Numeric COA segment helpers. Account keys are digit strings only — no English words.
 * <p>
 * fullNumber = entity(2) + type(2) + subType(2) + mainAccount + subAccount(4) + buffer(2) + currency(3)
 * Example: {@code 10200010001000000344} = entity 10, LIABILITY 20, sub 00, main 10001, leaf 0000, buf 00, HKD 344.
 */
public final class CoaCodes {
    public static final String ENTITY = "10";
    public static final String SUB_TYPE = "00";
    public static final String BUFFER = "00";
    public static final String PRIMARY_SUB = "0000";

    private CoaCodes() {}

    public static String typeCode(CoaType type) {
        if (type == null) {
            return typeCodeLiability();
        }
        return switch (type) {
            case ASSET -> "10";
            case LIABILITY -> typeCodeLiability();
            case EQUITY -> "30";
            case REVENUE -> "40";
            case EXPENSE -> "50";
        };
    }

    /** LIABILITY segment used by member wallets / pool (default COA). */
    public static String typeCodeLiability() {
        return "20";
    }

    /**
     * ISO 4217 numeric currency code when known; loyalty/crypto fall back to stable 3-digit codes.
     */
    public static String currencyCode(Currency currency) {
        if (currency == null) {
            return "000";
        }
        return switch (currency) {
            case USD -> "840";
            case HKD -> "344";
            case CNY, CNH -> "156";
            case EUR -> "978";
            case GBP -> "826";
            case JPY -> "392";
            case SGD -> "702";
            case AUD -> "036";
            case CAD -> "124";
            case CHF -> "756";
            case TWD -> "901";
            case KRW -> "410";
            case THB -> "764";
            case MYR -> "458";
            case IDR -> "360";
            case PHP -> "608";
            case INR -> "356";
            case VND -> "704";
            case LP -> "999";
            default -> _fallbackCurrencyCode(currency.getIsoCode());
        };
    }

    public static String fullNumber(
        String entity,
        String type,
        String subType,
        String mainAccount,
        String subAccount,
        String buffer,
        Currency currency
    ) {
        return entity + type + subType + mainAccount + subAccount + buffer + currencyCode(currency);
    }

    public static String fullNumber(String mainAccount, String subAccount, CoaType coaType, Currency currency) {
        return fullNumber(ENTITY, typeCode(coaType), SUB_TYPE, mainAccount, subAccount, BUFFER, currency);
    }

    /**
     * Leaf code: numeric refCode (e.g. 89 → 0089) or zero-padded sequential index.
     */
    public static String subAccountCode(String refCode, int sequentialFallback) {
        if (refCode != null && refCode.matches("\\d{1,4}")) {
            return String.format("%04d", Integer.parseInt(refCode));
        }
        int n = Math.max(0, sequentialFallback);
        if (n > 9999) {
            n = n % 10000;
        }
        return String.format("%04d", n);
    }

    public static boolean isPrimarySub(String subAccount) {
        return subAccount == null || PRIMARY_SUB.equals(subAccount) || "0".equals(subAccount);
    }

    private static String _fallbackCurrencyCode(String iso) {
        if (iso == null || iso.isBlank()) {
            return "000";
        }
        int h = Math.floorMod(iso.trim().toUpperCase().hashCode(), 900) + 100;
        return String.format("%03d", h);
    }
}
