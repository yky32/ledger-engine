package com.altech.ledger.entity.json_context;

import java.math.BigDecimal;

/**
 * Optional bank/fee breakdown attached to a deposit movement detail payload.
 */
public record DepositDetailMetadata(
    String bankName,
    String reference,
    BigDecimal bankChargeFee,
    String bankChargeFeeCurrency,
    BigDecimal handlingFee,
    String handlingFeeCurrency
) {}
