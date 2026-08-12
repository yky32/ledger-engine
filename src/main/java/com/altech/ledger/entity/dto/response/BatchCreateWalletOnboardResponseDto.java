package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    /** ownerIds that already had a wallet. */
    private List<String> alreadyExistingOwnerIds;
}
