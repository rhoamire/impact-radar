package com.impactradar.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.impactradar.dto.RelationshipCandidateResponse;

@Repository
public class RelationshipCandidateRepository {

    private final JdbcClient jdbcClient;

    public RelationshipCandidateRepository(
            JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<RelationshipCandidateResponse> findByFile(
            UUID fileId) {

        return jdbcClient
            .sql("""
                SELECT
                    c.id,

                    c.source_file_id,
                    sf.name AS source_file_name,

                    c.target_file_id,
                    tf.name AS target_file_name,

                    c.source_version_id,
                    c.target_version_id,

                    c.similarity,
                    c.suggested_relationship_type,
                    c.confidence,
                    c.evidence,
                    c.model_name,
                    c.status,
                    c.created_at,
                    c.reviewed_at

                FROM relationship_candidates c

                JOIN files sf
                    ON sf.id = c.source_file_id

                JOIN files tf
                    ON tf.id = c.target_file_id

                WHERE
                    c.source_file_id = :fileId
                    OR c.target_file_id = :fileId

                ORDER BY
                    CASE
                        WHEN c.status = 'CANDIDATE'
                            THEN 0
                        WHEN c.status = 'ACCEPTED'
                            THEN 1
                        ELSE 2
                    END,
                    c.similarity DESC
                """)
            .param("fileId", fileId)
            .query((rs, rowNum) ->
                new RelationshipCandidateResponse(
                    rs.getObject("id", UUID.class),

                    rs.getObject(
                        "source_file_id",
                        UUID.class
                    ),

                    rs.getString(
                        "source_file_name"
                    ),

                    rs.getObject(
                        "target_file_id",
                        UUID.class
                    ),

                    rs.getString(
                        "target_file_name"
                    ),

                    rs.getObject(
                        "source_version_id",
                        UUID.class
                    ),

                    rs.getObject(
                        "target_version_id",
                        UUID.class
                    ),

                    rs.getBigDecimal("similarity"),

                    rs.getString(
                        "suggested_relationship_type"
                    ),

                    rs.getBigDecimal("confidence"),

                    rs.getString("evidence"),

                    rs.getString("model_name"),

                    rs.getString("status"),

                    rs.getObject(
                        "created_at",
                        java.time.OffsetDateTime.class
                    ),

                    rs.getObject(
                        "reviewed_at",
                        java.time.OffsetDateTime.class
                    )
                )
            )
            .list();
    }

      public void accept(UUID candidateId) {
          jdbcClient
              .sql("""
                  SELECT accept_relationship_candidate(
                      :candidateId
                  )
                  """)
              .param("candidateId", candidateId)
              .query((rs, rowNum) -> rs.getObject(1))
              .list();
      }

      public void reject(UUID candidateId) {
          jdbcClient
              .sql("""
                  SELECT reject_relationship_candidate(
                      :candidateId
                  )
                  """)
              .param("candidateId", candidateId)
              .query((rs, rowNum) -> rs.getObject(1))
              .list();
      }

      public void undo(UUID candidateId) {
          jdbcClient
              .sql("""
                  SELECT undo_relationship_candidate(
                      :candidateId
                  )
                  """)
              .param("candidateId", candidateId)
              .query((rs, rowNum) -> rs.getObject(1))
              .list();
      }
}