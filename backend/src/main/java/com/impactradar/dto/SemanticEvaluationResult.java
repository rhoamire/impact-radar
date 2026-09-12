package com.impactradar.dto;

public record SemanticEvaluationResult(
        int truePositives,
        int falsePositives,
        int falseNegatives,
        int unresolvedCandidates,
        double precision,
        double recall,
        double f1
) {
}