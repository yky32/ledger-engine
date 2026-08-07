package com.altech.core.constant.enu;

import com.altech.core.exception.BizException;
import com.altech.core.response.SystemResponse;
import lombok.Getter;

import java.math.RoundingMode;
import java.util.Arrays;

public enum Currency {

    // =========== CurrencyType.FIAT
    JPY("JPY", 0, RoundingMode.DOWN, CurrencyType.FIAT),
    HKD("HKD", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CNH("CNH", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CNY("CNY", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    USD("USD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    GBP("GBP", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    AUD("AUD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    EUR("EUR", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CAD("CAD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CHF("CHF", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    SGD("SGD", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    SEK("SEK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    KRW("KRW", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    NOK("NOK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    NZD("NZD", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    INR("INR", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    MXN("MXN", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    TWD("TWD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    ZAR("ZAR", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    BRL("BRL", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    DKK("DKK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    PLN("PLN", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    THB("THB", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    ILS("ILS", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    IDR("IDR", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CZK("CZK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    AED("AED", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    TRY("TRY", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    HUF("HUF", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CLP("CLP", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    SAR("SAR", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    PHP("PHP", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    MYR("MYR", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    COP("COP", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    RUB("RUB", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    RON("RON", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    PEN("PEN", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    BHD("BHD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    BGN("BGN", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    ARS("ARS", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    VND("VND", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    RDN("RDN", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    NDK("NDK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),

    // =========== CurrencyType.CRYPTO
    // majors
    BTC("BTC", 8, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    ETH("ETH", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    // L1 / L2
    SOL("SOL", 9, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    ADA("ADA", 6, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    XRP("XRP", 6, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    DOGE("DOGE", 8, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    DOT("DOT", 10, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    AVAX("AVAX", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    MATIC("MATIC", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    POL("POL", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    TRX("TRX", 6, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    TON("TON", 9, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    NEAR("NEAR", 24, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    ATOM("ATOM", 6, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    LTC("LTC", 8, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    BCH("BCH", 8, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    XLM("XLM", 7, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    ALGO("ALGO", 6, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    APT("APT", 8, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    SUI("SUI", 9, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    // exchange / wrapped
    BNB("BNB", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    WBTC("WBTC", 8, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    WETH("WETH", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    // stablecoins
    USDT("USDT", 6, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    USDC("USDC", 6, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    DAI("DAI", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    FDUSD("FDUSD", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    TUSD("TUSD", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    BUSD("BUSD", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    // DeFi / others (common)
    LINK("LINK", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    UNI("UNI", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    AAVE("AAVE", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    SHIB("SHIB", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    PEPE("PEPE", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),

    // =========== CurrencyType.LOYALTY_POINT
    LP("LP", 0, RoundingMode.DOWN, CurrencyType.LOYALTY_POINT),


    ALL("ALL", 2, RoundingMode.HALF_UP, CurrencyType.ALL)
    ;


    @Getter
    private final String isoCode;
    @Getter
    private final int decimalPlaces;
    @Getter
    private final RoundingMode roundingMode;
    @Getter
    private final CurrencyType type;

    Currency(String isoCode, int decimalPlaces, RoundingMode roundingMode, CurrencyType type) {
        this.isoCode = isoCode;
        this.decimalPlaces = decimalPlaces;
        this.roundingMode = roundingMode;
        this.type = type;
    }

    /**
     * Resolve from JSON/path/db code ({@code USD}, {@code LP}, {@code BTC}, …).
     */
    public static Currency get(String input) {
        if (input == null || input.isBlank()) {
            throw new BizException(SystemResponse.PAM0400, "Currency is required");
        }
        String code = input.trim();
        for (Currency value : Currency.values()) {
            if (code.equalsIgnoreCase(value.name()) || code.equalsIgnoreCase(value.isoCode)) {
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", code, code, Arrays.asList(Currency.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }

    public boolean isFiat() {
        return type == CurrencyType.FIAT;
    }

    public boolean isCrypto() {
        return type == CurrencyType.CRYPTO;
    }

    public boolean isLoyaltyPoint() {
        return type == CurrencyType.LOYALTY_POINT;
    }

    @Override
    public String toString() {
        return isoCode;
    }
}

