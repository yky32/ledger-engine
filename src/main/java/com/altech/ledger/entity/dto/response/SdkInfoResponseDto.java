package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Handshake for ledger-engine-sdk compatibility checks. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdkInfoResponseDto {
    private String engineVersion;
    private String product;
    /** Minimum SDK version recommended for full feature set. */
    private String minSdkVersion;
    private String recommendedSdkVersion;
    @Builder.Default
    private Map<String, Boolean> features = new LinkedHashMap<>();
}
