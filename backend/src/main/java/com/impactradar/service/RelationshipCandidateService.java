package com.impactradar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.impactradar.dto.RelationshipCandidateResult;
import com.impactradar.dto.RelationshipEvidence;
import com.impactradar.dto.SemanticSearchResult;
import com.impactradar.model.FileVersion;
import com.impactradar.repository.DocumentRepository;
import com.impactradar.repository.SemanticSearchRepository;

@Service
public class RelationshipCandidateService {

    private static final double DEFAULT_MIN_SIMILARITY = 0.40;
    private static final int DEFAULT_LIMIT = 5;
    private static final String EMBEDDING_MODEL = "all-minilm";

    private final SemanticSearchRepository semanticSearchRepository;
    private final DocumentRepository documentRepository;
    private final RelationshipEvidenceService evidenceService;

    public RelationshipCandidateService(
            SemanticSearchRepository semanticSearchRepository,
            DocumentRepository documentRepository,
            RelationshipEvidenceService evidenceService
    ) {
        this.semanticSearchRepository = semanticSearchRepository;
        this.documentRepository = documentRepository;
        this.evidenceService = evidenceService;
    }

    public List<RelationshipCandidateResult> discoverCandidates(
            UUID fileId
    ) {
        FileVersion sourceVersion =
                documentRepository.findCurrentVersion(fileId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No current version found for file: " + fileId
                                )
                        );

        String sourceFileName =
                documentRepository.findFileName(fileId);

        List<SemanticSearchResult> semanticCandidates =
                semanticSearchRepository.findCandidateDocuments(
                        fileId,
                        DEFAULT_LIMIT,
                        DEFAULT_MIN_SIMILARITY
                );

        List<RelationshipCandidateResult> results =
                new ArrayList<>();

        for (SemanticSearchResult candidate : semanticCandidates) {

            FileVersion candidateVersion =
                    documentRepository.findCurrentVersion(
                            candidate.fileId()
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "No current version found for file: "
                                            + candidate.fileId()
                            )
                    );

            RelationshipEvidence evidence =
                    evidenceService.analyze(
                            sourceFileName,
                            sourceVersion.content(),
                            candidate.fileName(),
                            candidateVersion.content()
                    );

            /*
             * Semantic search direction:
             *
             *     searched file -> candidate
             *
             * does NOT necessarily equal relationship direction.
             *
             * Example:
             *
             *     Search from QA
             *          finds
             *     API
             *
             * Evidence can say:
             *
             *     QA -> depends_on -> API
             */
            UUID relationshipSourceId;
            UUID relationshipTargetId;
            UUID relationshipSourceVersionId;
            UUID relationshipTargetVersionId;

            switch (evidence.directionalSignal()) {

                case "SOURCE_DEPENDS_ON_TARGET" -> {
                    relationshipSourceId = fileId;
                    relationshipTargetId = candidate.fileId();

                    relationshipSourceVersionId = sourceVersion.id();
                    relationshipTargetVersionId = candidateVersion.id();
                }

                case "TARGET_DEPENDS_ON_SOURCE" -> {
                    relationshipSourceId = candidate.fileId();
                    relationshipTargetId = fileId;

                    relationshipSourceVersionId = candidateVersion.id();
                    relationshipTargetVersionId = sourceVersion.id();
                }

                case "SOURCE_REFERENCES_TARGET" -> {
                    relationshipSourceId = fileId;
                    relationshipTargetId = candidate.fileId();

                    relationshipSourceVersionId = sourceVersion.id();
                    relationshipTargetVersionId = candidateVersion.id();
                }

                case "TARGET_REFERENCES_SOURCE" -> {
                    relationshipSourceId = candidate.fileId();
                    relationshipTargetId = fileId;

                    relationshipSourceVersionId = candidateVersion.id();
                    relationshipTargetVersionId = sourceVersion.id();
                }

                default -> {
                    /*
                     * We don't actually know the relationship direction.
                     *
                     * In this case, do not invent one.
                     *
                     * The candidate is still returned, but we don't
                     * persist it because there is no trustworthy direction.
                     */
                    results.add(
                            new RelationshipCandidateResult(
                                    fileId,
                                    candidate.fileId(),
                                    candidate.fileName(),
                                    candidate.similarity(),
                                    null,
                                    0.0,
                                    evidence.directionalSignal(),
                                    evidence.sharedIdentifiers(),
                                    evidence.explanation()
                            )
                    );

                    continue;
                }
            }

            semanticSearchRepository.saveCandidate(
                    relationshipSourceId,
                    relationshipTargetId,
                    relationshipSourceVersionId,
                    relationshipTargetVersionId,
                    candidate.similarity().doubleValue(),
                    evidence.suggestedRelationshipType(),
                    evidence.evidenceConfidence(),
                    evidence.explanation(),
                    EMBEDDING_MODEL
            );

            results.add(
                    new RelationshipCandidateResult(
                            relationshipSourceId,
                            relationshipTargetId,
                            candidate.fileName(),
                            candidate.similarity(),
                            evidence.suggestedRelationshipType(),
                            evidence.evidenceConfidence(),
                            evidence.directionalSignal(),
                            evidence.sharedIdentifiers(),
                            evidence.explanation()
                    )
            );
        }

        return results;
    }
}