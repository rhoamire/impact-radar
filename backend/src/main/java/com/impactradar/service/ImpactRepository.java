package com.impactradar.service;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.impactradar.dto.ImpactPredictionResponse;

@Repository
public class ImpactRepository {

    private final JdbcClient jdbcClient;

    public ImpactRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ImpactPredictionResponse> analyzeAndStore(
            UUID changeEventId,
            UUID fileId
    ) {
        List<ImpactPredictionResponse> predictions =
                jdbcClient
                        .sql("""
                                SELECT
                                    affected_file_id,
                                    affected_file_name,
                                    impact_depth,
                                    path_count,
                                    min_confidence,
                                    impact_score,
                                    impact_level,
                                    path,
                                    relationship_path,
                                    provenance_path
                                FROM analyze_impact(:fileId)
                                """)
                        .param("fileId", fileId)
                        .query((rs, rowNum) ->
                                new ImpactPredictionResponse(
                                        rs.getObject("affected_file_id", UUID.class),
                                        rs.getString("affected_file_name"),
                                        rs.getInt("impact_depth"),
                                        rs.getInt("path_count"),
                                        rs.getBigDecimal("min_confidence"),
                                        rs.getBigDecimal("impact_score"),
                                        rs.getString("impact_level"),
                                        rs.getString("path"),
                                        rs.getString("relationship_path"),
                                        rs.getString("provenance_path")
                                )
                        )
                        .list();

        for (ImpactPredictionResponse prediction : predictions) {
            jdbcClient
                    .sql("""
                            INSERT INTO impact_predictions (
                                change_event_id,
                                affected_file_id,
                                impact_depth,
                                impact_score,
                                impact_level,
                                min_confidence,
                                path,
                                relationship_path,
                                path_count
                            )
                            VALUES (
                                :changeEventId,
                                :affectedFileId,
                                :impactDepth,
                                :impactScore,
                                :impactLevel,
                                :minConfidence,
                                :path,
                                :relationshipPath,
                                :pathCount
                            )
                            """)
                    .param("changeEventId", changeEventId)
                    .param("affectedFileId", prediction.affectedFileId())
                    .param("impactDepth", prediction.impactDepth())
                    .param("impactScore", prediction.impactScore())
                    .param("impactLevel", prediction.impactLevel())
                    .param("minConfidence", prediction.minConfidence())
                    .param("path", prediction.path())
                    .param("relationshipPath", prediction.relationshipPath())
                    .param("pathCount", prediction.pathCount())
                    .update();
        }

        return predictions;
    }
}