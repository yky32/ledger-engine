package com.altech.ledger.entity.dto.response;

/**
 * One configuration row (name, target, scope, value).
 */
public record GetSystemConfigurationResponseDto(
    Long id,
    String name,
    String target,
    String scope,
    String value
) {}
