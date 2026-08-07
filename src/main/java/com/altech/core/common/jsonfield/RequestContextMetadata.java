package com.altech.core.common.jsonfield;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REQUEST_CONTEXT: ip, user-agent, device, api, request-id, headers, path, requstBody, start_dt, end_dt
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestContextMetadata {
    private String ip;
    private String userAgent;
    private String startDt;
    private String endDt;
    private String apiUrl;
    private String requestId; // x-request-id
    private Object requestHeaders;
    private Object methodArguments;
}
