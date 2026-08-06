package com.altech.core.exception;

import com.altech.core.response.Response;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BizException extends RuntimeException {
    private Response response;
    private Object data;

    public BizException(Response response) {
        super(response.getMessage());
        this.response = response;
    }

    public BizException(Response response, String message) {
        super(message);
        this.response = response;
        this.data = Map.of("detail", message);
    }

    public <T, U> BizException(Response response, Map<T, U> map) {
        super(response.getMessage());
        this.response = response;
        this.data = map;
    }

    public <T> BizException(Response response, T data) {
        super(response.getMessage());
        this.response = response;
        if (data instanceof String s) {
            this.data = Map.of("detail", s);
        } else {
            this.data = Objects.requireNonNullElseGet(data, Map::of);
        }
    }
}
