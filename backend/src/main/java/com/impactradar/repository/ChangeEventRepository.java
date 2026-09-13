package com.impactradar.repository;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ChangeEventRepository {

    private final JdbcClient jdbcClient;

    public ChangeEventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public UUID findChangeEventIdByVersion(UUID versionId) {
        return jdbcClient
                .sql("""
                        SELECT id
                        FROM change_events
                        WHERE new_version_id = :versionId
                        LIMIT 1
                        """)
                .param("versionId", versionId)
                .query(UUID.class)
                .optional()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No change event found for version: " + versionId
                        )
                );
    }
}