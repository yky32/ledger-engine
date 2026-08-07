package com.altech.ledger.entity.dto.response;

/**
 * Lightweight ops counters for wallets / accounts / movements.
 */
public record GetDashboardResponseDto(
    long walletCount,
    long accountCount,
    long movementCount,
    long openMovementCount
) {}
