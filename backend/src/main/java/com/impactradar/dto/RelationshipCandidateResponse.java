package com.impactradar.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RelationshipCandidateResponse(
    UUID id,

    UUID sourceFileId,
    String sourceFileName,

    UUID targetFileId,
    String targetFileName,

    UUID sourceVersionId,
    UUID targetVersionId,

    BigDecimal similarity,
    String suggestedRelationshipType,
    BigDecimal confidence,
    String evidence,
    String modelName,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime reviewedAt
) {}