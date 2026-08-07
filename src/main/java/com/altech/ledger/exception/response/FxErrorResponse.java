package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/** FX rate domain errors. */
public interface FxErrorResponse {
    Response FX0404 = new Response("FX0404", "Fx rate not found.", HttpStatus.NOT_FOUND);
    Response FX0409 = new Response("FX0409", "Fx rate already exists.", HttpStatus.CONFLICT);
}
