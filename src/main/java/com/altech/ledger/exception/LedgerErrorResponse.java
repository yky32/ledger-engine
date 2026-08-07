package com.altech.ledger.exception;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Domain error codes (TGT-style {@code XXX0nnn} constants) for {@link LedgerException} / {@code BizException}.
 */
public interface LedgerErrorResponse {

    // ===== WAL = wallet onboarding / wallet =====
    Response WAL0400 = new Response("WAL0400", "Invalid wallet request.", HttpStatus.BAD_REQUEST);
    Response WAL0404 = new Response("WAL0404", "Wallet not found.", HttpStatus.NOT_FOUND);
    Response WAL0409 = new Response("WAL0409", "Wallet already onboarded.", HttpStatus.CONFLICT);
    Response WAL0403 = new Response("WAL0403", "Wallet is not active.", HttpStatus.CONFLICT);

    // ===== ACC = account =====
    Response ACC0404 = new Response("ACC0404", "Account not found.", HttpStatus.NOT_FOUND);
    Response ACC0409 = new Response("ACC0409", "Account already exists.", HttpStatus.CONFLICT);
    Response ACC0400 = new Response("ACC0400", "Invalid account request.", HttpStatus.BAD_REQUEST);

    // ===== MOV = movement =====
    Response MOV0404 = new Response("MOV0404", "Movement not found.", HttpStatus.NOT_FOUND);
    Response MOV0400 = new Response("MOV0400", "Invalid movement request.", HttpStatus.BAD_REQUEST);
    Response MOV0409 = new Response("MOV0409", "Movement conflict.", HttpStatus.CONFLICT);

    // ===== GEN = generic =====
    Response GEN0400 = new Response("GEN0400", "Bad request.", HttpStatus.BAD_REQUEST);
    Response GEN0404 = new Response("GEN0404", "Not found.", HttpStatus.NOT_FOUND);
    Response GEN0409 = new Response("GEN0409", "Conflict.", HttpStatus.CONFLICT);
}
