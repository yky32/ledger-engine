package com.altech.ledger.entity.dto.response;

/**
 * One configuration row (name, target, scope, value).
 * {@code value} is JSON (object / array / scalar).
 */
public record GetSystemConfigurationResponseDto(
    Long id,
    String name,
    String target,
    String scope,
    Object value
) {}
