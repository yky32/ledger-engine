package com.altech.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Result <T> {
    @JsonUnwrapped
    private Response response;
    private T data;
    private String requestId;
    private Pagination pagination;

    public Result(Response response) {
        this.response = response;
    }

    public Result(Response response, T data) {
        this.response = response;
        this.data = data;
    }

    public Result(Response response, T data, Pagination pagination) {
        this.response = response;
        this.data = data;
        this.pagination = pagination;
    }
}