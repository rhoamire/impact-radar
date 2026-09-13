package com.impactradar.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class RelationshipExplanationService {

    private final JdbcClient jdbcClient;

    public RelationshipExplanationService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public RelationshipExplanation explain(
            UUID fileA,
            UUID fileB
    ) {
        return jdbcClient
                .sql("""
                        SELECT
                            source_file_id,
                            source.name AS source_file_name,
                            target_file_id,
                            target.name AS target_file_name,
                            relationship_type,
                            provenance,
                            confidence,
                            evidence
                        FROM file_relationships r
                        JOIN files source
                            ON source.id = r.source_file_id
                        JOIN files target
                            ON target.id = r.target_file_id
                        WHERE
                            (r.source_file_id = :fileA
                             AND r.target_file_id = :fileB)
                            OR
                            (r.source_file_id = :fileB
                             AND r.target_file_id = :fileA)
                        ORDER BY r.created_at DESC
                        LIMIT 1
                        """)
                .param("fileA", fileA)
                .param("fileB", fileB)
                .query((rs, rowNum) ->
                        new RelationshipExplanation(
                                rs.getObject("source_file_id", UUID.class),
                                rs.getString("source_file_name"),
                                rs.getObject("target_file_id", UUID.class),
                                rs.getString("target_file_name"),
                                rs.getString("relationship_type"),
                                rs.getString("provenance"),
                                rs.getBigDecimal("confidence"),
                                rs.getString("evidence")
                        )
                )
                .optional()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No authoritative relationship exists between "
                                        + fileA + " and " + fileB
                        )
                );
    }

    public record RelationshipExplanation(
            UUID sourceFileId,
            String sourceFileName,
            UUID targetFileId,
            String targetFileName,
            String relationshipType,
            String provenance,
            BigDecimal confidence,
            String evidence
    ) {
    }
}