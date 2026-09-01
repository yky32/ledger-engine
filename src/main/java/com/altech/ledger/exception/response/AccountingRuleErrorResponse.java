package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/** Accounting rule domain errors. */
public interface AccountingRuleErrorResponse {
    Response RUL0404 = new Response("RUL0404", "Accounting rule not found.", HttpStatus.NOT_FOUND);
    Response RUL0409 = new Response("RUL0409", "Accounting rule already exists.", HttpStatus.CONFLICT);
}
