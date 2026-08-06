package com.altech.ledger.entity.json_context.remarks;

/** Port of the-wallet-ledger RemarkNotesMetadata. */
public record RemarkNotesMetadata(
    String note,
    String author,
    String createdAt
) {}
