package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/** Account / COA domain errors. */
public interface AccountErrorResponse {
    Response ACC0400 = new Response("ACC0400", "Invalid account request.", HttpStatus.BAD_REQUEST);
    Response ACC0404 = new Response("ACC0404", "Account not found.", HttpStatus.NOT_FOUND);
    Response ACC0409 = new Response("ACC0409", "Account already exists.", HttpStatus.CONFLICT);
}
