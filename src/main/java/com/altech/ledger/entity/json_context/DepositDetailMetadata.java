package com.altech.ledger.entity.json_context;

import java.math.BigDecimal;

/** Port of the-wallet-ledger DepositDetailMetadata. */
public record DepositDetailMetadata(
    String bankName,
    String reference,
    BigDecimal bankChargeFee,
    String bankChargeFeeCurrency,
    BigDecimal handlingFee,
    String handlingFeeCurrency
) {}
