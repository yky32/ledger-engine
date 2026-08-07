package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/** Ledger movement domain errors. */
public interface MovementErrorResponse {
    Response MOV0400 = new Response("MOV0400", "Invalid movement request.", HttpStatus.BAD_REQUEST);
    Response MOV0403 = new Response("MOV0403", "Insufficient balance.", HttpStatus.CONFLICT);
    Response MOV0404 = new Response("MOV0404", "Movement not found.", HttpStatus.NOT_FOUND);
    Response MOV0409 = new Response("MOV0409", "Movement conflict.", HttpStatus.CONFLICT);
}
