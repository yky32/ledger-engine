package com.altech.ledger.usecase.ingest;

import com.altech.ledger.entity.po.ingest.IngestPolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnsureWalletCoaResolveTest {

    @Test
    void metadataCoaWins() {
        IngestPolicy p = new IngestPolicy();
        p.setAutoWalletCoaProfileCode("UAF_LOAN");
        assertThat(EnsureWalletForIngestUseCase.resolveCoaProfileCode(
            p, Map.of("coaProfileCode", "UAF_CC"))).isEqualTo("UAF_CC");
    }

    @Test
    void productStreamMaps() {
        IngestPolicy p = new IngestPolicy();
        assertThat(EnsureWalletForIngestUseCase.resolveCoaProfileCode(
            p, Map.of("productStream", "CC"))).isEqualTo("UAF_CC");
        assertThat(EnsureWalletForIngestUseCase.resolveCoaProfileCode(
            p, Map.of("productStream", "loan"))).isEqualTo("UAF_LOAN");
    }

    @Test
    void doorFallback() {
        IngestPolicy p = new IngestPolicy();
        p.setAutoWalletCoaProfileCode("UAF_CC");
        assertThat(EnsureWalletForIngestUseCase.resolveCoaProfileCode(p, Map.of()))
            .isEqualTo("UAF_CC");
    }

    @Test
    void blankIsDefault() {
        assertThat(EnsureWalletForIngestUseCase.resolveCoaProfileCode(new IngestPolicy(), Map.of()))
            .isNull();
    }
}
