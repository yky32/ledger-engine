package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

public interface CoaErrorResponse {
    Response COA0400 = new Response("COA0400", "Invalid COA profile request.", HttpStatus.BAD_REQUEST);
    Response COA0404 = new Response("COA0404", "COA profile not found.", HttpStatus.NOT_FOUND);
    Response COA0409 = new Response("COA0409", "COA profile code already exists.", HttpStatus.CONFLICT);
}
