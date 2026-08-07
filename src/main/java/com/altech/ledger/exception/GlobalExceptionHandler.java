package com.altech.ledger.exception;

import com.altech.core.exception.BaseGlobalExceptionHandler;
import com.altech.core.response.R;
import com.altech.core.response.SystemResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Service-level advice. BizException is handled in {@link BaseGlobalExceptionHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> illegalArg(IllegalArgumentException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
            R.fail(SystemResponse.PAM0400, ex.getMessage()),
            SystemResponse.PAM0400.getHttpStatus());
    }
}
