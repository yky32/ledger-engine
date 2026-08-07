package com.altech.ledger.exception.response;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Wallet domain errors (TGT {@code *ErrorResponse} pattern).
 */
public interface WalletErrorResponse {
    Response WAL0400 = new Response("WAL0400", "Invalid wallet request.", HttpStatus.BAD_REQUEST);
    Response WAL0403 = new Response("WAL0403", "Wallet is not active.", HttpStatus.CONFLICT);
    Response WAL0404 = new Response("WAL0404", "Wallet not found.", HttpStatus.NOT_FOUND);
    Response WAL0409 = new Response("WAL0409", "Wallet already onboarded.", HttpStatus.CONFLICT);
}
