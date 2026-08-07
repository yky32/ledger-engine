package com.altech.ledger.entity.dto.parity;

/**
 * System configuration and ops dashboard API.
 */
public final class SystemDtos {
    private SystemDtos() {}

    /**
     * One configuration row (name, target, scope, value).
     */
    public record ConfigurationResponse(
        Long id,
        String name,
        String target,
        String scope,
        String value
    ) {}

    /**
     * Lightweight ops counters for wallets / accounts / movements.
     */
    public record DashboardResponse(
        long walletCount,
        long accountCount,
        long movementCount,
        long openMovementCount
    ) {}
}
