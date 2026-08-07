package com.altech.core.response;

import org.springframework.http.HttpStatus;

public interface SystemResponse {
    // ===== SYS = System, 0000-1999 ==> expect return, like redirect, ok.
    Response SYS0000 = new Response("SYS0000", "Success.");
    Response SYS0301 = new Response("SYS0301", "Redirect is required.", HttpStatus.MOVED_PERMANENTLY);
    // ===== SYS = System, 0000-1999 ==> expect return, like redirect, ok.

    // ===== SAU = System Authentication...
    Response SAU0400 = new Response("SAU0400", "Invalid Username / Credentials.", HttpStatus.UNAUTHORIZED);
    Response SAU0401 = new Response("SAU0401", "AccessDeniedException.", HttpStatus.UNAUTHORIZED);
    Response SAU0403 = new Response("SAU0403", "AuthenticationException.", HttpStatus.UNAUTHORIZED);
    Response SAU0499 = new Response("SAU0499", "Error in getting new token.", HttpStatus.INTERNAL_SERVER_ERROR);
    // ===== SAU = System Authentication...

    // ===== SYS = System, 9000-9999 ==> server errors, internal server error, method mismatch, api client error etc.
    Response SYM9400 = new Response("SYM9400", "API Client Error.", HttpStatus.BAD_REQUEST);
    Response SYS9400 = new Response("SYS9400", "API Client Error.", HttpStatus.BAD_REQUEST);
    Response SYS9401 = new Response("SYS9401", "API Client Error.", HttpStatus.UNAUTHORIZED);
    Response SYS9403 = new Response("SYS9403", "API Client Error.", HttpStatus.UNAUTHORIZED);
    Response SYS9499 = new Response("SYS9499", "API Client Error.", HttpStatus.INTERNAL_SERVER_ERROR);
    Response SYS9503 = new Response("SYS9503", "Service Unavailable.", HttpStatus.SERVICE_UNAVAILABLE);
    Response SYS9901 = new Response("SYS9901", "SQL Error.", HttpStatus.INTERNAL_SERVER_ERROR);
    Response SYS9902 = new Response("SYS9902", "Redis Key not existed / expired.", HttpStatus.BAD_REQUEST);
    Response SYS9405 = new Response("SYS9405", "API Method Error.", HttpStatus.METHOD_NOT_ALLOWED);
    Response SYS9429 = new Response("SYS9429", "Sorry. Be Patient. ", HttpStatus.TOO_MANY_REQUESTS);
    Response SYS9998 = new Response("SYS9998", "Fail. in [system error]. plz check log in kibana/loki.", HttpStatus.INTERNAL_SERVER_ERROR);
    Response SYS9999 = new Response("SYS9999", "Fail.", HttpStatus.INTERNAL_SERVER_ERROR);
    // ===== SYS = System, 9000-9999 ==> server errors, internal server error, method mismatch, api client error etc.

    // ===== PAM = Parameter issue....
    Response PAM0400 = new Response("PAM0400", "Invalid field validation.", HttpStatus.BAD_REQUEST);
    // ===== PAM = Parameter issue....


    // ===== TET = Tenancy
    Response TET0400 = new Response("TET0400", "Tenant id / key not found.", HttpStatus.BAD_REQUEST);
    Response TET0404 = new Response("TET0404", "TenantContext null in [PO] @PrePersist.", HttpStatus.BAD_REQUEST);
    // ===== TET = Tenancy
}