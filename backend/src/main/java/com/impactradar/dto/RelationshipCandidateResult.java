package com.impactradar.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RelationshipCandidateResult(
        UUID sourceFileId,
        UUID targetFileId,
        String searchedTargetFileName,
        BigDecimal similarity,
        String suggestedRelationshipType,
        double confidence,
        String directionalSignal,
        List<String> sharedIdentifiers,
        String evidence
) {
}