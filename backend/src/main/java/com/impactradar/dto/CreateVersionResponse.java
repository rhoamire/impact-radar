package com.impactradar.dto;

import java.util.List;
import java.util.UUID;

public record CreateVersionResponse(
        UUID versionId,
        int versionNumber,
        UUID changeEventId,
        String diff,
        int linesAdded,
        int linesRemoved,
        List<ImpactPredictionResponse> impact
) {
}