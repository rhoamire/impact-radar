package com.impactradar.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VersionSummaryResponse(
    UUID versionId,
    int versionNumber,
    boolean current,
    OffsetDateTime createdAt,
    UUID changeEventId,
    int linesAdded,
    int linesRemoved
) {}