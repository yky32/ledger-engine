package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UafDemoSeedResponseDto {
    private List<GetCoaProfileResponseDto> profiles;
    private List<GetWalletOnboardResponseDto> wallets;
    private String note;
}
