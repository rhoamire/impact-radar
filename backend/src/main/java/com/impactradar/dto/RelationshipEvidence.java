package com.impactradar.dto;

import java.util.List;

public record RelationshipEvidence(
        List<String> sharedIdentifiers,
        boolean sourceMentionsTarget,
        boolean targetMentionsSource,
        boolean sourceHasDependencyLanguage,
        boolean targetHasDependencyLanguage,
        String directionalSignal,
        String suggestedRelationshipType,
        double evidenceConfidence,
        String explanation
) {
}