package com.altech.ledger.entity.dto.request;

/**
 * Activation payload (optional workflow / account refs).
 */
public record ActivateLedgerWalletRequestDto(
    String accountId,
    String workflowExecutionId
) {}
