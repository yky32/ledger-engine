package com.altech.ledger.exception;

import com.altech.core.exception.BaseGlobalExceptionHandler;
import com.altech.core.response.R;
import com.altech.core.response.Response;
import com.altech.core.response.Result;
import com.altech.core.response.SystemResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(LedgerException.class)
    public ResponseEntity<Object> ledger(LedgerException ex, HttpServletRequest request) {
        Response response = new Response(ex.getCode(), ex.getMessage(), ex.getStatus());
        Result<Object> body = withRequestId(R.error(response, null));
        return new ResponseEntity<>(body, ex.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> illegalArg(IllegalArgumentException ex, HttpServletRequest request) {
        Result<Object> body = withRequestId(R.fail(SystemResponse.PAM0400, ex.getMessage()));
        return new ResponseEntity<>(body, SystemResponse.PAM0400.getHttpStatus());
    }
}
