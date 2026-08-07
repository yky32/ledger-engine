package com.altech.ledger.exception;

import com.altech.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Domain exception carrying a structured {@link Response} code (TGT {@code BizException} shape).
 */
public class LedgerException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Response response;

    public LedgerException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.response = new Response(code, message, status);
    }

    public LedgerException(Response response) {
        super(response.getMessage());
        this.response = response;
        this.status = response.getHttpStatus() != null ? response.getHttpStatus() : HttpStatus.BAD_REQUEST;
        this.code = response.getCode();
    }

    public LedgerException(Response response, String detail) {
        super(detail != null && !detail.isBlank() ? detail : response.getMessage());
        this.response = new Response(response.getCode(),
            detail != null && !detail.isBlank() ? detail : response.getMessage(),
            response.getHttpStatus() != null ? response.getHttpStatus() : HttpStatus.BAD_REQUEST);
        this.status = this.response.getHttpStatus();
        this.code = this.response.getCode();
    }

    public static LedgerException of(Response response) {
        return new LedgerException(response);
    }

    public static LedgerException of(Response response, String detail) {
        return new LedgerException(response, detail);
    }

    public static LedgerException notFound(String message) {
        return new LedgerException(LedgerErrorResponse.GEN0404, message);
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

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Response getResponse() {
        return response;
    }
}
