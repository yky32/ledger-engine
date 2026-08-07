package com.altech.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class JSONUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static boolean isValidJSON(String jsonStr) {
        try {
            OBJECT_MAPPER.readTree(jsonStr);
        } catch (IOException ie) {
            return false;
        }
        return true;
    }

    public static <T> T convertValue(Map map, Class<T> valueType) {
        return CallableUtil.call(() -> OBJECT_MAPPER.convertValue(map, valueType));
    }

    public static <T> T convertValue(Map map, TypeReference<T> valueType) {
        return CallableUtil.call(() -> OBJECT_MAPPER.convertValue(map, valueType));
    }

    public static <T> T readValue(InputStream content, Class<T> valueType) {
        return CallableUtil.call(() -> OBJECT_MAPPER.readValue(content, valueType));
    }

    public static <T> T readValue(InputStream content, TypeReference<T> valueType) {
        return CallableUtil.call(() -> OBJECT_MAPPER.readValue(content, valueType));
    }

    public static <T> T readValue(String content, Class<T> valueType) {
        return CallableUtil.call(() -> OBJECT_MAPPER.readValue(content, valueType));
    }

    public static <T> T readValue(String content, TypeReference<T> valueType) {
        return CallableUtil.call(() -> OBJECT_MAPPER.readValue(content, valueType));
    }

    public static <T> T convertFromObject(Object object, Class<T> valueType) {
        return CallableUtil.call(() -> OBJECT_MAPPER.convertValue(object, valueType));
    }

    public static <T> List<T> convertListFromObject(Object object, Class<T[]> valueType) {
        return CallableUtil.call(() -> {
            T[] array = OBJECT_MAPPER.convertValue(object, valueType);
            return List.of(array);
        });
    }

    public static String writeValue(Object obj) {
        return CallableUtil.call(() -> OBJECT_MAPPER.writeValueAsString(obj));
    }

    public static <T> T convertFromObject(Object object, TypeReference<T> valueType) {
        return CallableUtil.call(() -> OBJECT_MAPPER.convertValue(object, valueType));
    }
    public static JsonNode readTree(String content) {
        return CallableUtil.call(() -> OBJECT_MAPPER.readTree(content));
    }
}
