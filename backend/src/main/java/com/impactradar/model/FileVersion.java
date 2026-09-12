package com.impactradar.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FileVersion(
        UUID id,
        UUID fileId,
        int versionNumber,
        String content,
        String contentHash,
        OffsetDateTime createdAt,
        boolean current
) {
}