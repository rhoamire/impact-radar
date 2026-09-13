CREATE OR REPLACE FUNCTION analyze_impact(p_file_id UUID)

RETURNS TABLE (
    root_file_id UUID,
    affected_file_id UUID,
    affected_file_name TEXT,
    impact_depth INTEGER,
    path TEXT,
    min_confidence NUMERIC(5,4),
    relationship_count INTEGER
)

LANGUAGE sql

AS $$

WITH RECURSIVE impact AS (

    -- First hop from the changed document.
    SELECT
        p_file_id AS root_file_id,
        r.target_file_id AS affected_file_id,
        1 AS impact_depth,

        ARRAY[
            p_file_id,
            r.target_file_id
        ]::UUID[] AS visited,

        ARRAY[
            r.relationship_type::TEXT
        ] AS rel_path,

        /*
         * Explicit relationships have NULL confidence because
         * they are declared rather than probabilistic.
         *
         * For impact propagation, an explicit relationship is
         * treated as full-strength (1.0000).
         *
         * Inferred relationships use their actual confidence.
         */
        COALESCE(
            r.confidence,
            1.0000
        )::NUMERIC(5,4) AS min_confidence,

        1 AS relationship_count

    FROM file_relationships r

    WHERE r.source_file_id = p_file_id


    UNION ALL


    -- Continue walking downstream relationships.
    SELECT
        i.root_file_id,
        r.target_file_id,
        i.impact_depth + 1,

        i.visited || r.target_file_id,

        i.rel_path || r.relationship_type::TEXT,

        LEAST(
            i.min_confidence,
            COALESCE(r.confidence, 1.0000)
        )::NUMERIC(5,4),

        i.relationship_count + 1

    FROM impact i

    JOIN file_relationships r
        ON r.source_file_id = i.affected_file_id

    WHERE NOT r.target_file_id = ANY(i.visited)
)


SELECT DISTINCT ON (affected_file_id)

    i.root_file_id,
    i.affected_file_id,
    f.name AS affected_file_name,
    i.impact_depth,

    array_to_string(
        i.rel_path,
        ' -> '
    ) AS path,

    i.min_confidence,

    i.relationship_count

FROM impact i

JOIN files f
    ON f.id = i.affected_file_id

ORDER BY
    affected_file_id,
    min_confidence DESC,
    impact_depth ASC;

$$;


COMMENT ON FUNCTION analyze_impact(UUID) IS
'Walks typed downstream relationships recursively. Explicit relationships are treated as full-strength for impact propagation, while inferred relationships use their evidence confidence. The returned min_confidence is an effective path reliability used for ranking, not a probability of failure.';