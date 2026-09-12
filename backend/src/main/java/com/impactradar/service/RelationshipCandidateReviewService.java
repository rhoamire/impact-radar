package com.impactradar.service;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class RelationshipCandidateReviewService {

    private final JdbcClient jdbcClient;

    public RelationshipCandidateReviewService(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public void review(
            UUID candidateId,
            String status
    ) {
        switch (status.toUpperCase()) {
            case "ACCEPTED" -> accept(candidateId);
            case "REJECTED" -> reject(candidateId);
            default -> throw new IllegalArgumentException(
                    "Status must be ACCEPTED or REJECTED"
            );
        }
    }

    private void accept(UUID candidateId) {

    jdbcClient
            .sql("""
                    SELECT accept_relationship_candidate(
                        :candidateId
                    )
                    """)
            .param("candidateId", candidateId)
            .query((rs, rowNum) -> null)
            .list();
    }

    private void reject(UUID candidateId) {

    jdbcClient
            .sql("""
                    SELECT reject_relationship_candidate(
                        :candidateId
                    )
                    """)
            .param("candidateId", candidateId)
            .query((rs, rowNum) -> null)
            .list();
    }
}