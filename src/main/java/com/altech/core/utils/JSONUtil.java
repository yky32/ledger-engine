package com.altech.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared Jackson helpers (aligned with tgt.app-core / backend style).
 */
public class JSONUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public static ObjectMapper mapper() {
        return OBJECT_MAPPER;
    }

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

    /**
     * Coerce any JSON-like value (Map / JsonNode / POJO / JSON string) into a mutable
     * {@code Map<String,Object>} — preferred for jsonb config blobs (e.g. digestion formula).
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object raw) {
        if (raw == null) {
            return new LinkedHashMap<>();
        }
        if (raw instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) {
                return new LinkedHashMap<>();
            }
            if (t.startsWith("{")) {
                return readValue(t, MAP_TYPE);
            }
            throw new IllegalArgumentException("Expected JSON object string, got: " + s);
        }
        if (raw instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            return out;
        }
        Map<String, Object> converted = convertFromObject(raw, MAP_TYPE);
        return converted == null ? new LinkedHashMap<>() : new LinkedHashMap<>(converted);
    }

    /** Pretty-ish single-line JSON for logs / movement descriptions. */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String s) {
            return s;
        }
        return writeValue(obj);
    }
}
