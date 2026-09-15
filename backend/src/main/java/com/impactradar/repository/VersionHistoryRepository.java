package com.impactradar.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.impactradar.dto.ChangeDetailResponse;
import com.impactradar.dto.VersionSummaryResponse;

@Repository
public class VersionHistoryRepository {

    private final JdbcClient jdbcClient;

    public VersionHistoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<VersionSummaryResponse> findVersions(
            UUID fileId) {

        return jdbcClient
            .sql("""
                SELECT
                    fv.id AS version_id,
                    fv.version_number,
                    fv.is_current,
                    fv.created_at,
                    ce.id AS change_event_id,
                    COALESCE(ce.lines_added, 0) AS lines_added,
                    COALESCE(ce.lines_removed, 0) AS lines_removed
                FROM file_versions fv
                LEFT JOIN change_events ce
                    ON ce.new_version_id = fv.id
                WHERE fv.file_id = :fileId
                ORDER BY fv.version_number DESC
                """)
            .param("fileId", fileId)
            .query((rs, rowNum) ->
                new VersionSummaryResponse(
                    rs.getObject(
                        "version_id",
                        UUID.class
                    ),

                    rs.getInt("version_number"),

                    rs.getBoolean("is_current"),

                    rs.getObject(
                        "created_at",
                        java.time.OffsetDateTime.class
                    ),

                    rs.getObject(
                        "change_event_id",
                        UUID.class
                    ),

                    rs.getInt("lines_added"),

                    rs.getInt("lines_removed")
                )
            )
            .list();
    }

    public ChangeDetailResponse findChange(
            UUID versionId) {

        return jdbcClient
            .sql("""
                SELECT
                    ce.id AS change_event_id,
                    ce.file_id,
                    f.name AS file_name,

                    ce.previous_version_id,
                    pv.version_number
                        AS previous_version_number,

                    ce.new_version_id,
                    nv.version_number
                        AS new_version_number,

                    ce.created_at,
                    ce.change_summary,
                    ce.diff_text,
                    ce.lines_added,
                    ce.lines_removed

                FROM change_events ce

                JOIN files f
                    ON f.id = ce.file_id

                JOIN file_versions nv
                    ON nv.id = ce.new_version_id

                LEFT JOIN file_versions pv
                    ON pv.id = ce.previous_version_id

                WHERE ce.new_version_id = :versionId

                LIMIT 1
                """)
            .param("versionId", versionId)
            .query((rs, rowNum) ->
                new ChangeDetailResponse(
                    rs.getObject(
                        "change_event_id",
                        UUID.class
                    ),

                    rs.getObject(
                        "file_id",
                        UUID.class
                    ),

                    rs.getString("file_name"),

                    rs.getObject(
                        "previous_version_id",
                        UUID.class
                    ),

                    (Integer) rs.getObject(
                        "previous_version_number"
                    ),

                    rs.getObject(
                        "new_version_id",
                        UUID.class
                    ),

                    rs.getInt("new_version_number"),

                    rs.getObject(
                        "created_at",
                        java.time.OffsetDateTime.class
                    ),

                    rs.getString("change_summary"),

                    rs.getString("diff_text"),

                    rs.getInt("lines_added"),

                    rs.getInt("lines_removed")
                )
            )
            .optional()
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "No change event found for version: "
                        + versionId
                )
            );
    }
}