package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

public interface DigestionErrorResponse {
    Response DIG0400 = new Response("DIG0400", "Invalid digestion rule request.", HttpStatus.BAD_REQUEST);
    Response DIG0404 = new Response("DIG0404", "Digestion rule not found.", HttpStatus.NOT_FOUND);
    Response DIG0409 = new Response("DIG0409", "Digestion rule code already exists.", HttpStatus.CONFLICT);
}
