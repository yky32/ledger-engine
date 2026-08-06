package com.altech.core.exception;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.core.response.SystemResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Shared structured-error handling for {@link R}/{@link Result} envelopes.
 * Service handlers may extend this and add domain exceptions.
 */
@Slf4j
public abstract class BaseGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request
    ) {
        if (ex.getCause() instanceof InvalidFormatException ife
            && ife.getTargetType() != null
            && ife.getTargetType().isEnum()) {
            String fieldName = !ife.getPath().isEmpty() ? ife.getPath().get(0).getFieldName() : "unknown";
            String value = String.valueOf(ife.getValue());
            String validValues = Arrays.toString(ife.getTargetType().getEnumConstants());
            String message = String.format(
                "Field '%s' with value '%s' is invalid! Accepted values are: %s",
                fieldName, value, validValues);
            return new ResponseEntity<>(R.fail(SystemResponse.PAM0400, message), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(R.fail(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Object> bizError(HttpServletRequest req, BizException ex) {
        Result<Object> body = withRequestId(R.error(ex.getResponse(), ex.getData()));
        HttpStatus status = ex.getResponse().getHttpStatus() != null
            ? ex.getResponse().getHttpStatus()
            : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> integrity(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(
            R.fail(SystemResponse.SYS9901, ex.getMostSpecificCause().getMessage()),
            SystemResponse.SYS9901.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> systemError(HttpServletRequest req, Exception exception) {
        log.error("Unhandled error on {}", req.getRequestURI(), exception);
        Result<String> error = withRequestId(R.error(SystemResponse.SYS9999, exception.getMessage()));
        return new ResponseEntity<>(error, SystemResponse.SYS9999.getHttpStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request
    ) {
        List<String> details = new ArrayList<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            details.add(error.getDefaultMessage());
        }
        return new ResponseEntity<>(R.fail(SystemResponse.PAM0400, details), HttpStatus.BAD_REQUEST);
    }

    protected <T> Result<T> withRequestId(Result<T> result) {
        result.setRequestId(UUID.randomUUID().toString());
        return result;
    }
}
