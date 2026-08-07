package com.altech.core.common.jsonfield;

import com.altech.core.aop.log.LogScope;
import com.altech.core.constant.enu.LogType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * TYPE: activity / audit trail
 * SCOPE: int / ext
 * REQUEST_CONTEXT: ip, user-agent, device, api, request-id, headers, path, requstBody, start_dt, end_dt
 * SYSTEM: uaa, payment-service, tenant-service, util
 * DOMAIN: user
 * EVENT: user.created
 * CONTENT: logging content
 * NEW_CONTENT: delta change
 * DELTA: _________________ differences
 * ACTION_BY: triggered
 * TRAFFIC_TIME: 1s.
 * METADATA: others info.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogContextMetadata{
    private RequestContextMetadata requestContext;
    private String topic;
    // PO fields
    private LogType type;
    private LogScope logScope;
    private String scope; //
    private String system;
    private String domain;
    private String event;

    // ====
    private Object requestBody; // method req
    private Object responseBody; // method res
    // ====

    // ====
    private Object content; // log content
    private Object newContent; // log content after audited
    private Object delta;
    // ====


    private String actionBy; // = userId
    private Long trafficTimeInMilliseconds; // = userId
    private Object metadata;
    private String traceId;
    private String correlationId; // This ID need to be set by different implementation
}
