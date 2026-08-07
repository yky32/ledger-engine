package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/** Accounting rule domain errors. */
public interface RuleErrorResponse {
    Response RUL0404 = new Response("RUL0404", "Rule not found.", HttpStatus.NOT_FOUND);
    Response RUL0409 = new Response("RUL0409", "Rule already exists.", HttpStatus.CONFLICT);
}
