package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCoaDictionaryRequestDto(
    @Size(max = 16) String kind,
    @Size(max = 32) String code,
    @Size(max = 200) String name,
    @Size(max = 2000) String definition,
    @Size(max = 120) String example,
    @Size(max = 16) String side
) {}
