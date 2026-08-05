package com.altech.ledger.exception;

import org.springframework.http.HttpStatus;

public class LedgerException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public LedgerException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static LedgerException notFound(String message) {
        return new LedgerException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static LedgerException notFound(String code, String message) {
        return new LedgerException(HttpStatus.NOT_FOUND, code, message);
    }

    public static LedgerException badRequest(String code, String message) {
        return new LedgerException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static LedgerException conflict(String code, String message) {
        return new LedgerException(HttpStatus.CONFLICT, code, message);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
