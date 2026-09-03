package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.json_context.WalletTierBand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WalletTierBandMathTest {

    private static final List<WalletTierBand> BANDS = List.of(
        new WalletTierBand("NONE", BigDecimal.ZERO, null),
        new WalletTierBand("SILVER", new BigDecimal("1000"), null),
        new WalletTierBand("GOLD", new BigDecimal("10000"), new BigDecimal("800")),
        new WalletTierBand("PLATINUM", new BigDecimal("50000"), new BigDecimal("8000"))
    );

    @Test
    void upgradeFromNone() {
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "NONE", new BigDecimal("1000")))
            .isEqualTo("SILVER");
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "NONE", new BigDecimal("10000")))
            .isEqualTo("GOLD");
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "GOLD", new BigDecimal("60000")))
            .isEqualTo("PLATINUM");
    }

    @Test
    void hysteresisKeepsGold() {
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "GOLD", new BigDecimal("9000")))
            .isEqualTo("GOLD");
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "GOLD", new BigDecimal("1200")))
            .isEqualTo("GOLD");
    }

    @Test
    void downgradeWhenBelowBar() {
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "GOLD", new BigDecimal("500")))
            .isEqualTo("NONE");
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "SILVER", BigDecimal.ZERO))
            .isEqualTo("NONE");
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "PLATINUM", new BigDecimal("7000")))
            .isEqualTo("SILVER");
    }

    @Test
    void refundShapedDrop() {
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "GOLD", new BigDecimal("200")))
            .isEqualTo("NONE");
    }

    @Test
    void blankCurrentTreatsAsLowest() {
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, null, new BigDecimal("0")))
            .isEqualTo("NONE");
        assertThat(WalletTierPolicyUseCase.nextTier(BANDS, "", new BigDecimal("1500")))
            .isEqualTo("SILVER");
    }
}
