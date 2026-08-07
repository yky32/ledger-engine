package com.altech.ledger.entity.json_context;

/**
 * Uploaded file descriptor (name, type, size, URL) for MANUAL movement docs.
 */
public record FileMetadata(
    String fileName,
    String contentType,
    Long size,
    String url
) {}
