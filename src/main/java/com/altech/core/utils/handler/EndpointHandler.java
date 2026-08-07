package com.altech.core.utils.handler;

import com.altech.core.response.Result;
import com.altech.core.utils.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;

public class EndpointHandler {

    @SneakyThrows
    public static <T> void out(HttpServletResponse httpResponse, int httpStatus,  Result<T> response) {
        httpResponse.setStatus(httpStatus);
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpResponse.getWriter().write(JSONUtil.writeValue(response));
    }
}
