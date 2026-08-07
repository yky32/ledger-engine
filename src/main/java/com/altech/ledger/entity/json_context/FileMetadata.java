package com.altech.ledger.entity.json_context;

/** FileMetadata. */
public record FileMetadata(
    String fileName,
    String contentType,
    Long size,
    String url
) {}
