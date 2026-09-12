package com.impactradar.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.impactradar.dto.SemanticSearchResult;

@Repository
public class SemanticSearchRepository {

    private final JdbcClient jdbcClient;

    public SemanticSearchRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<SemanticSearchResult> findSimilarDocuments(
            UUID fileId,
            int limit
    ) {
        return jdbcClient
                .sql("""
                        WITH query_embedding AS (
                            SELECT dc.embedding
                            FROM document_chunks dc
                            JOIN file_versions fv
                                ON fv.id = dc.file_version_id
                            WHERE fv.file_id = :fileId
                              AND fv.is_current = TRUE
                            ORDER BY dc.chunk_index
                            LIMIT 1
                        ),
                        similarities AS (
                            SELECT
                                f.id AS file_id,
                                f.name AS file_name,
                                MAX(
                                    1 - (dc.embedding <=> qe.embedding)
                                ) AS similarity
                            FROM document_chunks dc
                            JOIN file_versions fv
                                ON fv.id = dc.file_version_id
                            JOIN files f
                                ON f.id = fv.file_id
                            CROSS JOIN query_embedding qe
                            WHERE fv.is_current = TRUE
                              AND f.id <> :fileId
                            GROUP BY f.id, f.name
                        )
                        SELECT
                            file_id,
                            file_name,
                            similarity
                        FROM similarities
                        ORDER BY similarity DESC
                        LIMIT :limit
                        """)
                .param("fileId", fileId)
                .param("limit", limit)
                .query((rs, rowNum) ->
                        new SemanticSearchResult(
                                rs.getObject("file_id", UUID.class),
                                rs.getString("file_name"),
                                rs.getBigDecimal("similarity")
                        )
                )
                .list();
    }

    public List<SemanticSearchResult> findCandidateDocuments(
            UUID fileId,
            int limit,
            double minSimilarity
    ) {
        return jdbcClient
                .sql("""
                        WITH query_embedding AS (
                            SELECT dc.embedding
                            FROM document_chunks dc
                            JOIN file_versions fv
                                ON fv.id = dc.file_version_id
                            WHERE fv.file_id = :fileId
                              AND fv.is_current = TRUE
                            ORDER BY dc.chunk_index
                            LIMIT 1
                        )
                        SELECT
                            f.id AS file_id,
                            f.name AS file_name,
                            MAX(
                                1 - (dc.embedding <=> qe.embedding)
                            ) AS similarity
                        FROM document_chunks dc
                        JOIN file_versions fv
                            ON fv.id = dc.file_version_id
                        JOIN files f
                            ON f.id = fv.file_id
                        CROSS JOIN query_embedding qe
                        WHERE fv.is_current = TRUE
                          AND f.id <> :fileId
                          AND NOT EXISTS (
                              SELECT 1
                              FROM file_relationships fr
                              WHERE
                                  (
                                      fr.source_file_id = :fileId
                                      AND fr.target_file_id = f.id
                                  )
                                  OR
                                  (
                                      fr.source_file_id = f.id
                                      AND fr.target_file_id = :fileId
                                  )
                          )
                        GROUP BY f.id, f.name
                        HAVING MAX(
                            1 - (dc.embedding <=> qe.embedding)
                        ) >= :minSimilarity
                        ORDER BY similarity DESC
                        LIMIT :limit
                        """)
                .param("fileId", fileId)
                .param("limit", limit)
                .param("minSimilarity", minSimilarity)
                .query((rs, rowNum) ->
                        new SemanticSearchResult(
                                rs.getObject("file_id", UUID.class),
                                rs.getString("file_name"),
                                rs.getBigDecimal("similarity")
                        )
                )
                .list();
    }

    public void saveCandidate(
            UUID sourceFileId,
            UUID targetFileId,
            UUID sourceVersionId,
            UUID targetVersionId,
            double similarity,
            String suggestedRelationshipType,
            double confidence,
            String evidence,
            String modelName
    ) {
        jdbcClient
                .sql("""
                        INSERT INTO relationship_candidates (
                            source_file_id,
                            target_file_id,
                            source_version_id,
                            target_version_id,
                            similarity,
                            suggested_relationship_type,
                            confidence,
                            evidence,
                            model_name,
                            status
                        )
                        VALUES (
                            :sourceFileId,
                            :targetFileId,
                            :sourceVersionId,
                            :targetVersionId,
                            :similarity,
                            CAST(:suggestedRelationshipType AS relationship_type),
                            :confidence,
                            :evidence,
                            :modelName,
                            'CANDIDATE'
                        )
                        ON CONFLICT (
                            source_file_id,
                            target_file_id,
                            source_version_id,
                            target_version_id
                        )
                        DO UPDATE SET
                            similarity = EXCLUDED.similarity,
                            suggested_relationship_type =
                                EXCLUDED.suggested_relationship_type,
                            confidence = EXCLUDED.confidence,
                            evidence = EXCLUDED.evidence,
                            model_name = EXCLUDED.model_name,
                            status = 'CANDIDATE'
                        """)
                .param("sourceFileId", sourceFileId)
                .param("targetFileId", targetFileId)
                .param("sourceVersionId", sourceVersionId)
                .param("targetVersionId", targetVersionId)
                .param("similarity", similarity)
                .param(
                        "suggestedRelationshipType",
                        suggestedRelationshipType
                )
                .param("confidence", confidence)
                .param("evidence", evidence)
                .param("modelName", modelName)
                .update();
    }
}