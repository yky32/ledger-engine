package com.altech.ledger.entity.dto.parity;

/** System configuration and ops dashboard API. */
public final class SystemDtos {
    private SystemDtos() {}

    public record ConfigurationResponse(
        Long id,
        String name,
        String target,
        String scope,
        String value
    ) {}

    public record DashboardResponse(
        long walletCount,
        long accountCount,
        long movementCount,
        long openMovementCount
    ) {}
}
