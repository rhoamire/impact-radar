package com.impactradar.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.impactradar.model.FileVersion;

@Repository
public class DocumentRepository {

    private final JdbcClient jdbcClient;

    private static final RowMapper<FileVersion> FILE_VERSION_MAPPER = (rs, rowNum) ->
            new FileVersion(
                    rs.getObject("id", UUID.class),
                    rs.getObject("file_id", UUID.class),
                    rs.getInt("version_number"),
                    rs.getString("content"),
                    rs.getString("content_hash"),
                    rs.getObject("created_at", java.time.OffsetDateTime.class),
                    rs.getBoolean("is_current")
            );

    public DocumentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<FileVersion> findCurrentVersion(UUID fileId) {
        return jdbcClient
                .sql("""
                        SELECT id, file_id, version_number, content,
                               content_hash, created_at, is_current
                        FROM file_versions
                        WHERE file_id = :fileId
                          AND is_current = TRUE
                        """)
                .param("fileId", fileId)
                .query(FILE_VERSION_MAPPER)
                .optional();
    }

    public void markNotCurrent(UUID versionId) {
        jdbcClient
                .sql("""
                        UPDATE file_versions
                        SET is_current = FALSE
                        WHERE id = :versionId
                        """)
                .param("versionId", versionId)
                .update();
    }

    public FileVersion insertVersion(
            UUID fileId,
            int versionNumber,
            String content,
            String contentHash
    ) {
        return jdbcClient
                .sql("""
                        INSERT INTO file_versions (
                            id,
                            file_id,
                            version_number,
                            content,
                            content_hash,
                            is_current
                        )
                        VALUES (
                            gen_random_uuid(),
                            :fileId,
                            :versionNumber,
                            :content,
                            :contentHash,
                            TRUE
                        )
                        RETURNING id, file_id, version_number, content,
                                  content_hash, created_at, is_current
                        """)
                .param("fileId", fileId)
                .param("versionNumber", versionNumber)
                .param("content", content)
                .param("contentHash", contentHash)
                .query(FILE_VERSION_MAPPER)
                .single();
    }

    public UUID recordChangeEvent(
            UUID fileId,
            UUID previousVersionId,
            UUID newVersionId,
            String summary,
            String diff,
            int linesAdded,
            int linesRemoved
    ) {
        return jdbcClient
                .sql("""
                        SELECT record_change_event(
                            :fileId,
                            :previousVersionId,
                            :newVersionId,
                            :summary,
                            :diff,
                            :linesAdded,
                            :linesRemoved
                        )
                        """)
                .param("fileId", fileId)
                .param("previousVersionId", previousVersionId)
                .param("newVersionId", newVersionId)
                .param("summary", summary)
                .param("diff", diff)
                .param("linesAdded", linesAdded)
                .param("linesRemoved", linesRemoved)
                .query(UUID.class)
                .single();
    }
}