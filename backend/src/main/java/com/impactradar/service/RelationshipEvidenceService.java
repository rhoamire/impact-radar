package com.impactradar.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.impactradar.dto.RelationshipEvidence;

@Service
public class RelationshipEvidenceService {

    /*
     * Technical identifiers we care about:
     *
     * /payments
     * payment_intent_id
     * payment_status
     * idempotency_key
     */
    private static final Pattern TECHNICAL_IDENTIFIER_PATTERN =
            Pattern.compile(
                    "(?<![A-Za-z0-9_])" +
                    "(" +
                        "/[A-Za-z0-9_./-]+" +
                        "|" +
                        "[A-Za-z][A-Za-z0-9]*_[A-Za-z0-9_]+" +
                    ")" +
                    "(?![A-Za-z0-9_])"
            );

    public RelationshipEvidence analyze(
            String sourceFileName,
            String sourceContent,
            String targetFileName,
            String targetContent
    ) {
        String sourceLower =
                sourceContent.toLowerCase(Locale.ROOT);

        String targetLower =
                targetContent.toLowerCase(Locale.ROOT);

        boolean sourceMentionsTarget =
                containsDocumentReference(
                        sourceLower,
                        targetFileName
                );

        boolean targetMentionsSource =
                containsDocumentReference(
                        targetLower,
                        sourceFileName
                );

        boolean sourceHasDependencyLanguage =
                containsDependencyLanguage(sourceLower);

        boolean targetHasDependencyLanguage =
                containsDependencyLanguage(targetLower);

        List<String> sharedIdentifiers =
                findSharedTechnicalIdentifiers(
                        sourceContent,
                        targetContent
                );

        String directionalSignal =
                determineDirection(
                        sourceLower,
                        targetLower,
                        sourceMentionsTarget,
                        targetMentionsSource
                );

        String suggestedRelationshipType =
                suggestRelationshipType(directionalSignal);

        double confidence =
                calculateEvidenceConfidence(
                        sourceMentionsTarget,
                        targetMentionsSource,
                        sourceHasDependencyLanguage,
                        targetHasDependencyLanguage,
                        sharedIdentifiers,
                        directionalSignal
                );

        String explanation =
                buildExplanation(
                        sharedIdentifiers,
                        sourceMentionsTarget,
                        targetMentionsSource,
                        directionalSignal
                );

        return new RelationshipEvidence(
                sharedIdentifiers,
                sourceMentionsTarget,
                targetMentionsSource,
                sourceHasDependencyLanguage,
                targetHasDependencyLanguage,
                directionalSignal,
                suggestedRelationshipType,
                confidence,
                explanation
        );
    }

    /**
     * Determine whether document content refers to another document.
     *
     * We first try the exact document name.
     *
     * If that fails, we also try the document's base name by removing
     * common document-type suffixes such as "Design", "Specification",
     * "Schema", "Runbook", and "Test Plan".
     *
     * Example:
     *
     * "Payment Service Design"
     *       becomes
     * "Payment Service"
     *
     * This allows natural references in prose without requiring authors
     * to repeat the complete database filename/title.
     */
    private boolean containsDocumentReference(
            String content,
            String fileName
    ) {
        String normalizedFileName =
                normalizePhrase(fileName);

        if (content.contains(normalizedFileName)) {
            return true;
        }

        String baseName =
                removeDocumentTypeSuffix(normalizedFileName);

        return !baseName.isBlank()
                && content.contains(baseName);
    }

    private String normalizePhrase(
            String text
    ) {
        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String removeDocumentTypeSuffix(
            String fileName
    ) {
        String result = fileName;

        result = result.replaceFirst(
                "\\s+test\\s+plan$",
                ""
        );

        result = result.replaceFirst(
                "\\s+(design|specification|schema|runbook)$",
                ""
        );

        return result.trim();
    }

    private boolean containsDependencyLanguage(
            String content
    ) {
        return content.contains("depends on")
                || content.contains("dependency")
                || content.contains("implements")
                || content.contains("consumes")
                || content.contains("validates")
                || content.contains("tests");
    }

    private List<String> findSharedTechnicalIdentifiers(
            String sourceContent,
            String targetContent
    ) {
        Set<String> sourceIdentifiers =
                extractTechnicalIdentifiers(sourceContent);

        Set<String> targetIdentifiers =
                extractTechnicalIdentifiers(targetContent);

        sourceIdentifiers.retainAll(targetIdentifiers);

        return sourceIdentifiers.stream()
                .sorted()
                .limit(20)
                .toList();
    }

    private Set<String> extractTechnicalIdentifiers(
            String content
    ) {
        Set<String> identifiers = new HashSet<>();

        Matcher matcher =
                TECHNICAL_IDENTIFIER_PATTERN.matcher(content);

        while (matcher.find()) {
            String identifier =
                    matcher.group(1)
                            .toLowerCase(Locale.ROOT);

            identifiers.add(
                    normalizeIdentifier(identifier)
            );
        }

        return identifiers;
    }

    private String normalizeIdentifier(
            String identifier
    ) {
        /*
         * Sentence punctuation should not become part of an identifier.
         *
         * "/payments."
         * "/payments,"
         *
         * both normalize to:
         *
         * "/payments"
         */
        if (identifier.startsWith("/")) {
            return identifier.replaceFirst(
                    "[.,;:!?]+$",
                    ""
            );
        }

        return identifier;
    }

    private String determineDirection(
        String sourceContent,
        String targetContent,
        boolean sourceMentionsTarget,
        boolean targetMentionsSource
) {
    boolean sourceUsesDependencyLanguage =
            containsDependencyLanguage(sourceContent);

    boolean targetUsesDependencyLanguage =
            containsDependencyLanguage(targetContent);

    /*
     * If the source document explicitly mentions the target and
     * uses language indicating implementation, consumption,
     * validation, testing, or dependency, the source is the
     * dependent document.
     *
     * Examples:
     *
     * Payment Service implements API Specification
     * QA Test Plan validates Payment Service
     */
    if (sourceMentionsTarget
            && sourceUsesDependencyLanguage) {

        return "SOURCE_DEPENDS_ON_TARGET";
    }

    /*
     * Same reasoning in the opposite direction.
     */
    if (targetMentionsSource
            && targetUsesDependencyLanguage) {

        return "TARGET_DEPENDS_ON_SOURCE";
    }

    /*
     * If there is a document reference but insufficient evidence
     * for dependency semantics, classify it as a reference.
     */
    if (sourceMentionsTarget) {
        return "SOURCE_REFERENCES_TARGET";
    }

    if (targetMentionsSource) {
        return "TARGET_REFERENCES_SOURCE";
    }

    return "UNDETERMINED";
}

    private String suggestRelationshipType(
            String directionalSignal
    ) {
        return switch (directionalSignal) {
            case "SOURCE_DEPENDS_ON_TARGET",
                 "TARGET_DEPENDS_ON_SOURCE" ->
                    "depends_on";

            case "SOURCE_REFERENCES_TARGET",
                 "TARGET_REFERENCES_SOURCE" ->
                    "references";

            default ->
                    null;
        };
    }

    private double calculateEvidenceConfidence(
        boolean sourceMentionsTarget,
        boolean targetMentionsSource,
        boolean sourceHasDependencyLanguage,
        boolean targetHasDependencyLanguage,
        List<String> sharedIdentifiers,
        String directionalSignal
) {
    double score = 0.0;

    /*
     * Shared identifiers are supporting evidence.
     * Their total contribution is capped.
     */
    score += Math.min(
            sharedIdentifiers.size() * 0.05,
            0.25
    );

    /*
     * A document explicitly mentioning the other document
     * is useful evidence that the pair is related.
     */
    if (sourceMentionsTarget) {
        score += 0.10;
    }

    if (targetMentionsSource) {
        score += 0.10;
    }

    /*
     * Dependency language only counts when it occurs together
     * with a mention of the candidate document.
     *
     * General words like "implements" or "depends on" elsewhere
     * in a document should not increase confidence for every pair.
     */
    if (sourceMentionsTarget && sourceHasDependencyLanguage) {
        score += 0.15;
    }

    if (targetMentionsSource && targetHasDependencyLanguage) {
        score += 0.15;
    }

    /*
     * A directional signal is especially valuable because it
     * distinguishes "related" from "A depends on B".
     */
    if (!"UNDETERMINED".equals(directionalSignal)) {
        score += 0.15;
    }

    /*
     * This is heuristic evidence, not a calibrated probability.
     * Never claim absolute certainty.
     */
    return Math.min(score, 0.95);
}

    private String buildExplanation(
            List<String> sharedIdentifiers,
            boolean sourceMentionsTarget,
            boolean targetMentionsSource,
            String directionalSignal
    ) {
        List<String> evidence = new ArrayList<>();

        if (!sharedIdentifiers.isEmpty()) {
            evidence.add(
                    "shared technical identifiers: "
                            + String.join(
                                    ", ",
                                    sharedIdentifiers
                            )
            );
        }

        if (sourceMentionsTarget) {
            evidence.add(
                    "source document mentions the target document"
            );
        }

        if (targetMentionsSource) {
            evidence.add(
                    "target document mentions the source document"
            );
        }

        if (!"UNDETERMINED".equals(directionalSignal)) {
            evidence.add(
                    "directional evidence: "
                            + directionalSignal
            );
        }

        if (evidence.isEmpty()) {
            return "Semantic similarity found, but no additional textual evidence was detected.";
        }

        return String.join("; ", evidence) + ".";
    }
}