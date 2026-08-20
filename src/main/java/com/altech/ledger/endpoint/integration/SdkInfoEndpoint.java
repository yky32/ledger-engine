package com.altech.ledger.endpoint.integration;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.SdkInfoResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SDK handshake — version + feature flags for client compatibility checks.
 */
@RestController
@RequestMapping("/integrations/sdk-info")
public class SdkInfoEndpoint {

    @Value("${spring.application.name:ledger-engine}")
    private String appName;

    @Value("${LEDGER_ENGINE_VERSION:1.0.0}")
    private String engineVersion;

    @GetMapping
    public Result<SdkInfoResponseDto> info() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("useCasesCatalog", true);
        features.put("webhookDryRun", true);
        features.put("balanceUpdatedKafka", true);
        features.put("factorSet", true);
        features.put("postingRecipes", true);
        features.put("coaTransactionCode", true);
        return R.success(SdkInfoResponseDto.builder()
            .product("LedgeRX")
            .engineVersion(engineVersion != null ? engineVersion : "1.0.0")
            .minSdkVersion("1.2.0")
            .recommendedSdkVersion("1.2.0")
            .features(features)
            .build());
    }
}
