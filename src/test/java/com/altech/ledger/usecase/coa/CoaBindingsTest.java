package com.altech.ledger.usecase.coa;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoaBindingsTest {

    @Test
    void defaultBindingsHasThreeRoles() {
        Map<String, Object> b = CoaBindings.defaultBindings();
        assertThat(b).containsKeys(
            CoaBindings.ROLE_MEMBER_SETTLEMENT,
            CoaBindings.ROLE_MEMBER_LP,
            CoaBindings.ROLE_PROGRAM_POOL);
        var sett = CoaBindings.require(b, CoaBindings.ROLE_MEMBER_SETTLEMENT);
        assertThat(sett.entity()).isEqualTo("10");
        assertThat(sett.type()).isEqualTo("20");
        assertThat(sett.currencyMode()).isEqualTo(CoaBindings.MODE_SETTLEMENT);
        var lp = CoaBindings.require(b, CoaBindings.ROLE_MEMBER_LP);
        assertThat(lp.currencyFixed()).isEqualTo("LP");
        var pool = CoaBindings.require(b, CoaBindings.ROLE_PROGRAM_POOL);
        assertThat(pool.allowNegative()).isTrue();
    }

    @Test
    void normalizeFillsMissingRoles() {
        Map<String, Object> partial = Map.of(
            CoaBindings.ROLE_MEMBER_LP,
            Map.of("entity", "01", "type", "99")
        );
        Map<String, Object> n = CoaBindings.normalize(partial);
        var lp = CoaBindings.require(n, CoaBindings.ROLE_MEMBER_LP);
        assertThat(lp.entity()).isEqualTo("01");
        assertThat(lp.type()).isEqualTo("99");
        assertThat(lp.subType()).isEqualTo("00");
        // settlement still default
        assertThat(CoaBindings.require(n, CoaBindings.ROLE_MEMBER_SETTLEMENT).entity()).isEqualTo("10");
    }
}
