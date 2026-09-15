package com.impactradar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.impactradar.dto.GraphEdge;
import com.impactradar.dto.GraphNode;
import com.impactradar.dto.ImpactGraphResponse;
import com.impactradar.dto.ImpactPredictionResponse;

@Repository
public class ImpactRepository {

    private final JdbcClient jdbcClient;

    public ImpactRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ImpactPredictionResponse> analyze(UUID fileId) {

    return jdbcClient
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
    public ImpactGraphResponse getGraph(UUID fileId) {

    List<ImpactPredictionResponse> predictions = analyze(fileId);

    List<GraphNode> nodes = new ArrayList<>();

    /*
     * Root node.
     */
    String rootName = jdbcClient
        .sql("""
            SELECT name
            FROM files
            WHERE id = :fileId
            """)
        .param("fileId", fileId)
        .query(String.class)
        .optional()
        .orElseThrow(() ->
            new IllegalArgumentException(
                "File not found: " + fileId
            )
        );

    GraphNode root = new GraphNode(
        fileId,
        rootName,
        0,
        null,
        "ROOT",
        "ROOT"
    );

    /*
     * Add affected nodes using the existing
     * deterministic impact analysis.
     */
    for (ImpactPredictionResponse prediction : predictions) {
        nodes.add(
            new GraphNode(
                prediction.affectedFileId(),
                prediction.affectedFileName(),
                prediction.impactDepth(),
                prediction.impactScore(),
                prediction.impactLevel(),
                prediction.provenancePath()
            )
        );
    }

    /*
     * Get every relationship edge reachable from
     * the selected root.
     *
     * Relationship direction:
     *
     * dependent -> dependency
     *
     * Impact traversal therefore starts from the
     * dependency and walks toward its dependents.
     */
    List<GraphEdge> edges = jdbcClient
        .sql("""
            WITH RECURSIVE reachable AS (
                SELECT
                    r.source_file_id,
                    r.target_file_id,
                    r.relationship_type,
                    r.provenance,
                    r.evidence,
                    ARRAY[
                        r.source_file_id,
                        r.target_file_id
                    ] AS visited
                FROM file_relationships r
                WHERE r.target_file_id = :fileId

                UNION ALL

                SELECT
                    r.source_file_id,
                    r.target_file_id,
                    r.relationship_type,
                    r.provenance,
                    r.evidence,
                    reachable.visited || r.source_file_id
                FROM file_relationships r
                JOIN reachable
                    ON r.target_file_id =
                       reachable.source_file_id
                WHERE NOT (
                    r.source_file_id = ANY(reachable.visited)
                )
            )
            SELECT DISTINCT
                source_file_id,
                target_file_id,
                relationship_type,
                provenance,
                evidence
            FROM reachable
            """)
        .param("fileId", fileId)
        .query((rs, rowNum) ->
            new GraphEdge(
                rs.getObject("source_file_id", UUID.class),
                rs.getObject("target_file_id", UUID.class),
                rs.getString("relationship_type"),
                rs.getString("provenance"),
                rs.getString("evidence")
            )
        )
        .list();

    return new ImpactGraphResponse(
        root,
        nodes,
        edges
    );
}
}