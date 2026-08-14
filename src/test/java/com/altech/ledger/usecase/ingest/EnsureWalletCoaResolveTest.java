package com.altech.ledger.usecase.ingest;

import com.altech.ledger.entity.po.ingest.IngestPolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnsureWalletCoaResolveTest {

    @Test
    void metadataCoaWins() {
        IngestPolicy p = new IngestPolicy();
        p.setAutoWalletCoaProfileCode("STREAM_B");
        assertThat(EnsureWalletForIngestUseCase.resolveCoaProfileCode(
            p, Map.of("coaProfileCode", "STREAM_A"))).isEqualTo("STREAM_A");
    }

    @Test
    void doorFallback() {
        IngestPolicy p = new IngestPolicy();
        p.setAutoWalletCoaProfileCode("STREAM_A");
        assertThat(EnsureWalletForIngestUseCase.resolveCoaProfileCode(p, Map.of()))
            .isEqualTo("STREAM_A");
    }

    @Test
    void blankIsDefault() {
        assertThat(EnsureWalletForIngestUseCase.resolveCoaProfileCode(new IngestPolicy(), Map.of()))
            .isNull();
    }
}
