package com.altech.ledger.usecase.coa;

import com.altech.ledger.util.CoaCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoaProfileDefaultsTest {

    @Test
    void legacyCodesMatchCoaCodes() {
        assertThat(CoaCodes.ENTITY).isEqualTo("10");
        assertThat(CoaCodes.typeCodeLiability()).isEqualTo("20");
        assertThat(CoaCodes.SUB_TYPE).isEqualTo("00");
        assertThat(CoaCodes.BUFFER).isEqualTo("00");
    }
}
