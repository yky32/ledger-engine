package com.altech.ledger.util;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;

/**
 * Numeric COA segment helpers. Account keys are digit strings only — no English words.
 * <p>
 * fullNumber = entity(2) + type(2) + subType(2) + mainAccount + buffer(2) + currency(3)
 * Example: {@code 1020001000100344} = entity 10, type 20, subType 00, main 10001, buffer 00, HKD 344.
 */
public final class CoaCodes {
    public static final String ENTITY = "10";
    public static final String SUB_TYPE = "00";
    public static final String BUFFER = "00";

    /** UA corporate operating account number (house main). */
    public static final String HOUSE_MAIN_ACCOUNT = "9999";

    private CoaCodes() {}

    /** House / company chart codes — not SDK eventTypes. */
    public static boolean isHouseCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String c = code.trim().toUpperCase(java.util.Locale.ROOT);
        return c.startsWith("HOUSE_") || c.startsWith("CORP_") || c.startsWith("GL_")
            || "PROGRAM".equals(c) || "HOUSE".equals(c);
    }

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
        String buffer,
        Currency currency
    ) {
        return entity + type + subType + mainAccount + buffer + currencyCode(currency);
    }

    public static String fullNumber(String mainAccount, CoaType coaType, Currency currency) {
        return fullNumber(ENTITY, typeCode(coaType), SUB_TYPE, mainAccount, BUFFER, currency);
    }

    private static String _fallbackCurrencyCode(String iso) {
        if (iso == null || iso.isBlank()) {
            return "000";
        }
        int h = Math.floorMod(iso.trim().toUpperCase().hashCode(), 900) + 100;
        return String.format("%03d", h);
    }
}
