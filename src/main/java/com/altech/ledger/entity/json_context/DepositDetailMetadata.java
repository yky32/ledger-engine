package com.altech.ledger.entity.json_context;

import java.math.BigDecimal;

/** DepositDetailMetadata. */
public record DepositDetailMetadata(
    String bankName,
    String reference,
    BigDecimal bankChargeFee,
    String bankChargeFeeCurrency,
    BigDecimal handlingFee,
    String handlingFeeCurrency
) {}
