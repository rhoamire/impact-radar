package com.impactradar.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ImpactPredictionResponse(
        UUID affectedFileId,
        String affectedFileName,
        int impactDepth,
        int pathCount,
        BigDecimal minConfidence,
        BigDecimal impactScore,
        String impactLevel,
        String path,
        String relationshipPath
) {
}