package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.enu.AccountSetStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** One AccountSet with nested CoA accounts (Phase A). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetAccountSetResponseDto {
    private Long id;
    private String code;
    private String name;
    private AccountSetStatus status;
    private List<GetWalletAccountResponseDto> accounts;
}
