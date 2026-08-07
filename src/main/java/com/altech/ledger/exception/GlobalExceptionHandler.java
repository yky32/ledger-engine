package com.altech.ledger.exception;

import com.altech.core.exception.BaseGlobalExceptionHandler;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.core.response.SystemResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Service-level advice. Domain errors use {@link com.altech.core.exception.BizException}
 * (handled in {@link BaseGlobalExceptionHandler}); add only extra mappings here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> illegalArg(IllegalArgumentException ex, HttpServletRequest request) {
        Result<Object> body = withRequestId(R.fail(SystemResponse.PAM0400, ex.getMessage()));
        return new ResponseEntity<>(body, SystemResponse.PAM0400.getHttpStatus());
    }
}
