package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk onboarding result: counts plus created wallets and already-existing user ids.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchCreateWalletOnboardResponseDto {
    private int requested;
    private int created;
    private int alreadyExists;
    private List<GetWalletOnboardResponseDto> createdWallets;
    private List<String> alreadyExistingUserIds;
}
