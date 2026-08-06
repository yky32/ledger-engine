package com.altech.ledger.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public final class MultipartFileMetadata {
    private MultipartFileMetadata() {}

    public static String summarize(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;
            parts.add(f.getOriginalFilename() + "|" + f.getContentType() + "|" + f.getSize());
        }
        return parts.isEmpty() ? null : String.join(";", parts);
    }
}
