package com.altech.core.exception;


import com.altech.core.api.ApiClient;
import com.altech.core.common.AppContextHolder;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.core.response.SystemResponse;
import com.altech.core.utils.handler.EndpointHandler;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;

@Slf4j
public class BaseGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private ApiClient apiClient;

    public BaseGlobalExceptionHandler(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public BaseGlobalExceptionHandler() {
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) ex.getCause();

            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String fieldName = !ife.getPath().isEmpty() ? ife.getPath().get(0).getFieldName() : "unknown";
                String value = String.valueOf(ife.getValue());
                String validValues = Arrays.toString(ife.getTargetType().getEnumConstants());
                String message = String.format("Field '%s' with value '%s' is invalid! Accepted values are: %s",
                        fieldName, value, validValues);

                return new ResponseEntity<>(R.fail(SystemResponse.PAM0400, message), HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<>(R.fail(ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> systemError(HttpServletRequest req, Exception exception) {
        Result<String> error = R.error(SystemResponse.SYS9999, exception.getMessage());
        if (apiClient != null) {
            apiClient.executeOnly(error);
        }
        return new ResponseEntity<>(error, SystemResponse.SYS9999.getHttpStatus());
    }

    @ExceptionHandler(value = BizException.class)
    public ResponseEntity<Object> error(HttpServletRequest req, BizException bizException) {
        // Set back the `x-request-id` for bug-tracing
        Result<Object> error = R.error(bizException.getResponse(), bizException.getData());
        setBackRequestId(error);
        if (apiClient != null) {
            apiClient.executeOnly(error);
        }
        return new ResponseEntity<>(error, bizException.getResponse().getHttpStatus());
    }

    private Result<Object> setBackRequestId(Result<Object> result) {
        String requestId = UUID.randomUUID().toString();
        try {
            requestId = AppContextHolder.CONTEXT.get()
                    .getRequestContext()
                    .getRequestId();
            log.info("-- request-id is set to be from [AppContext].requestId [{}]", requestId);
        } catch (Exception exception) {
            log.info("-- Error in request-id set from [AppContext], used back the default one [{}]", requestId);
        }
        result.setRequestId(requestId);
        log.error("-- BaseGlobalExceptionHandler, request-id => [{}], result => [{}]", requestId, result);
        return result;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (apiClient != null) {
            apiClient.executeOnly(new BizException(SystemResponse.SYS9405, ex.getMessage()));
        }
        List<String> details = new ArrayList<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            details.add(error.getDefaultMessage());
        }
        return new ResponseEntity<>(R.fail(details), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(R.fail(SystemResponse.SYS9901, ex.getMessage()), SystemResponse.SYS9901.getHttpStatus());
    }

    // __ only handle 401 issue with no token
    public void authenticationDenied(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        Map<String, String> detail = Map.of("path", request.getRequestURI(), "error", ex.getMessage());
        log.info("-- BaseGlobalExceptionHandler.authenticationDenied, ex => {}", detail);
        EndpointHandler.out(response, SystemResponse.SAU0403.getHttpStatus().value(), R.fail(SystemResponse.SAU0403, detail));
    }

    public void accessDenied(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        Map<String, String> detail = Map.of("path", request.getRequestURI(), "error", ex.getMessage());
        log.info("-- BaseGlobalExceptionHandler.accessDenied, ex => {}", detail);
        EndpointHandler.out(response, SystemResponse.SAU0401.getHttpStatus().value(), R.fail(SystemResponse.SAU0401, detail));
    }
}
