package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/** System configuration domain errors. */
public interface ConfigErrorResponse {
    Response CFG0404 = new Response("CFG0404", "Configuration not found.", HttpStatus.NOT_FOUND);
}
