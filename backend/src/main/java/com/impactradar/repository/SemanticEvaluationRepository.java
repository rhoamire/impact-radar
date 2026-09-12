package com.impactradar.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.impactradar.dto.SemanticEvaluationResult;

@Repository
public class SemanticEvaluationRepository {

    private final JdbcClient jdbcClient;

    public SemanticEvaluationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SemanticEvaluationResult evaluate() {

        EvaluationCounts counts = jdbcClient
                .sql("""
                        SELECT

                            -- True positives:
                            -- known positive cases for which V4 produced
                            -- the expected directed relationship.
                            (
                                SELECT COUNT(*)
                                FROM semantic_evaluation_cases ec
                                WHERE ec.relationship_exists = TRUE
                                  AND EXISTS (
                                      SELECT 1
                                      FROM relationship_candidates rc
                                      WHERE rc.source_file_id =
                                            ec.source_file_id
                                        AND rc.target_file_id =
                                            ec.target_file_id
                                        AND rc.suggested_relationship_type =
                                            ec.expected_relationship_type
                                  )
                            ) AS true_positives,

                            -- False negatives:
                            -- known positive cases for which V4 failed
                            -- to produce the expected relationship.
                            (
                                SELECT COUNT(*)
                                FROM semantic_evaluation_cases ec
                                WHERE ec.relationship_exists = TRUE
                                  AND NOT EXISTS (
                                      SELECT 1
                                      FROM relationship_candidates rc
                                      WHERE rc.source_file_id =
                                            ec.source_file_id
                                        AND rc.target_file_id =
                                            ec.target_file_id
                                        AND rc.suggested_relationship_type =
                                            ec.expected_relationship_type
                                  )
                            ) AS false_negatives,

                            -- False positives:
                            -- known negative cases for which V4 actually
                            -- proposed some relationship type.
                            (
                                SELECT COUNT(*)
                                FROM semantic_evaluation_cases ec
                                WHERE ec.relationship_exists = FALSE
                                  AND EXISTS (
                                      SELECT 1
                                      FROM relationship_candidates rc
                                      WHERE rc.source_file_id =
                                            ec.source_file_id
                                        AND rc.target_file_id =
                                            ec.target_file_id
                                        AND rc.suggested_relationship_type
                                            IS NOT NULL
                                  )
                            ) AS false_positives,

                            -- Unresolved candidates:
                            -- negative cases where semantic similarity
                            -- was sufficient to create a candidate, but
                            -- no relationship type was inferred.
                            (
                                SELECT COUNT(*)
                                FROM semantic_evaluation_cases ec
                                WHERE ec.relationship_exists = FALSE
                                  AND EXISTS (
                                      SELECT 1
                                      FROM relationship_candidates rc
                                      WHERE rc.source_file_id =
                                            ec.source_file_id
                                        AND rc.target_file_id =
                                            ec.target_file_id
                                        AND rc.suggested_relationship_type
                                            IS NULL
                                  )
                            ) AS unresolved_candidates
                        """)
                .query((rs, rowNum) ->
                        new EvaluationCounts(
                                rs.getInt("true_positives"),
                                rs.getInt("false_positives"),
                                rs.getInt("false_negatives"),
                                rs.getInt("unresolved_candidates")
                        )
                )
                .single();

        int tp = counts.truePositives();
        int fp = counts.falsePositives();
        int fn = counts.falseNegatives();

        double precision =
                tp + fp == 0
                        ? 0.0
                        : (double) tp / (tp + fp);

        double recall =
                tp + fn == 0
                        ? 0.0
                        : (double) tp / (tp + fn);

        double f1 =
                precision + recall == 0.0
                        ? 0.0
                        : 2.0 * precision * recall
                                / (precision + recall);

        return new SemanticEvaluationResult(
                tp,
                fp,
                fn,
                counts.unresolvedCandidates(),
                precision,
                recall,
                f1
        );
    }

    private record EvaluationCounts(
            int truePositives,
            int falsePositives,
            int falseNegatives,
            int unresolvedCandidates
    ) {
    }
}