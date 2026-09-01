package com.altech.ledger.entity.dto.response;

import java.util.List;

/** House chart + the one company wallet and its books. */
public record GetHouseBooksResponseDto(
    Long walletId,
    String ownerId,
    List<GetCoaProfileResponseDto> profiles,
    List<GetWalletAccountResponseDto> accounts
) {}
