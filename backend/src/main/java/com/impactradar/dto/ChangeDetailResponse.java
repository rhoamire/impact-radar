package com.impactradar.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChangeDetailResponse(
    UUID changeEventId,
    UUID fileId,
    String fileName,

    UUID previousVersionId,
    Integer previousVersionNumber,

    UUID newVersionId,
    int newVersionNumber,

    OffsetDateTime createdAt,

    String changeSummary,
    String diffText,

    int linesAdded,
    int linesRemoved
) {}