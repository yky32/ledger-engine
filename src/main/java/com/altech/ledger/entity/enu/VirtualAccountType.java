package com.altech.ledger.entity.enu;

/** Port of the-wallet-ledger {@code VirtualAccountType} (region codes only; currency sets deferred). */
public enum VirtualAccountType {
    HONG_KONG("HK"),
    UNITED_KINGDOM("UK"),
    AUSTRALIA("AU"),
    EUROPE("EU");

    private final String code;

    VirtualAccountType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
