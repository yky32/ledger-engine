package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

public interface IntegrationErrorResponse {
    Response ING0404 = new Response("ING0404", "Failed ingest record not found.", HttpStatus.NOT_FOUND);
}
