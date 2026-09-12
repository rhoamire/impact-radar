package com.impactradar.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AIGroundingRepository {

    private final JdbcClient jdbcClient;

    public AIGroundingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ChangeContext getChangeContext(UUID changeEventId) {
        return jdbcClient
                .sql("""
                        SELECT
                            ce.id,
                            f.name AS file_name,
                            ce.change_summary,
                            ce.diff_text,
                            ce.lines_added,
                            ce.lines_removed
                        FROM change_events ce
                        JOIN files f
                            ON f.id = ce.file_id
                        WHERE ce.id = :changeEventId
                        """)
                .param("changeEventId", changeEventId)
                .query((rs, rowNum) ->
                        new ChangeContext(
                                rs.getObject("id", UUID.class),
                                rs.getString("file_name"),
                                rs.getString("change_summary"),
                                rs.getString("diff_text"),
                                rs.getInt("lines_added"),
                                rs.getInt("lines_removed")
                        )
                )
                .single();
    }

    public ImpactContext getImpactContext(
            UUID changeEventId,
            UUID affectedFileId
    ) {
        return jdbcClient
                .sql("""
                        SELECT
                            ce.id AS change_event_id,
                            changed.name AS changed_file_name,
                            affected.name AS affected_file_name,
                            ip.impact_depth,
                            ip.path_count,
                            ip.min_confidence,
                            ip.impact_score,
                            ip.impact_level,
                            ip.path,
                            ip.relationship_path,
                            fr.relationship_type,
                            fr.confidence AS relationship_confidence,
                            fr.evidence
                        FROM impact_predictions ip
                        JOIN change_events ce
                            ON ce.id = ip.change_event_id
                        JOIN files changed
                            ON changed.id = ce.file_id
                        JOIN files affected
                            ON affected.id = ip.affected_file_id
                        LEFT JOIN file_relationships fr
                            ON fr.source_file_id = ce.file_id
                            AND fr.target_file_id = ip.affected_file_id
                        WHERE ip.change_event_id = :changeEventId
                          AND ip.affected_file_id = :affectedFileId
                        """)
                .param("changeEventId", changeEventId)
                .param("affectedFileId", affectedFileId)
                .query((rs, rowNum) ->
                        new ImpactContext(
                                rs.getObject("change_event_id", UUID.class),
                                rs.getString("changed_file_name"),
                                rs.getString("affected_file_name"),
                                rs.getInt("impact_depth"),
                                rs.getInt("path_count"),
                                rs.getBigDecimal("min_confidence"),
                                rs.getBigDecimal("impact_score"),
                                rs.getString("impact_level"),
                                rs.getString("path"),
                                rs.getString("relationship_path"),
                                rs.getString("relationship_type"),
                                rs.getBigDecimal("relationship_confidence"),
                                rs.getString("evidence")
                        )
                )
                .single();
    }

    public record ChangeContext(
            UUID changeEventId,
            String fileName,
            String changeSummary,
            String diff,
            int linesAdded,
            int linesRemoved
    ) {
    }

    public record ImpactContext(
            UUID changeEventId,
            String changedFileName,
            String affectedFileName,
            int impactDepth,
            int pathCount,
            java.math.BigDecimal minConfidence,
            java.math.BigDecimal impactScore,
            String impactLevel,
            String path,
            String relationshipPath,
            String relationshipType,
            java.math.BigDecimal relationshipConfidence,
            String evidence
    ) {
    }
}