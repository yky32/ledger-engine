package com.altech.ledger.entity.json_context.remarks;

/**
 * Free-text remark on a movement or related record (author + timestamp).
 */
public record RemarkNotesMetadata(
    String note,
    String author,
    String createdAt
) {}
