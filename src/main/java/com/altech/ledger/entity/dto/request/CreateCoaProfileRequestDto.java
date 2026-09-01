package com.altech.ledger.entity.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCoaProfileRequestDto(
    @NotBlank @Size(max = 40) String code,
    @Size(max = 200) String name,
    /** Optional. Omit or leave blank → same as {@code code}. Set only to extend (code ≠ eventType). */
    @Size(max = 64) String transactionCode,
    Boolean isDefault,
    Boolean isEnabled,
    @Size(max = 8) String entity,
    @Size(max = 8) String type,
    @Size(max = 8) String subType,
    @Size(max = 8) String buffer,
    @JsonAlias("lpCurrency") @Size(max = 16) String currency,
    Boolean poolAllowNegative,
    /** House books: bind to this company wallet. Member event COA omit. */
    Long walletId
) {}
