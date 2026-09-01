package com.altech.ledger.usecase.coa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoaBookResolverMainAccountTest {

    @Test
    void normalizeTrimsAndBlanksToNull() {
        assertThat(CoaBookResolver.normalizeMainAccount(null)).isNull();
        assertThat(CoaBookResolver.normalizeMainAccount("")).isNull();
        assertThat(CoaBookResolver.normalizeMainAccount("  ")).isNull();
        assertThat(CoaBookResolver.normalizeMainAccount(" 908964815317 ")).isEqualTo("908964815317");
    }
}
