package com.altech.ledger.entity.json_context;

/** Port of the-wallet-ledger FileMetadata. */
public record FileMetadata(
    String fileName,
    String contentType,
    Long size,
    String url
) {}
