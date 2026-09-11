-- Impact Radar V2
-- Relationship-aware impact scoring.

CREATE TABLE relationship_weights (
    relationship_type relationship_type PRIMARY KEY,
    impact_weight NUMERIC(5,4) NOT NULL
        CHECK (impact_weight >= 0 AND impact_weight <= 1),
    description TEXT NOT NULL
);

INSERT INTO relationship_weights (
    relationship_type,
    impact_weight,
    description
) VALUES
    ('depends_on',   1.0000, 'Strong dependency: changes are likely to require downstream review or modification.'),
    ('derived_from', 0.9500, 'Derived artifact: changes in the source can materially affect the derived document.'),
    ('supersedes',   0.9000, 'Replacement relationship: changes may invalidate or replace downstream assumptions.'),
    ('references',   0.5000, 'Informational reference: the document may need review, but is less likely to require implementation changes.');

-- The V1 function had a different return shape.
-- Drop it so V2 can return the additional scoring fields.
DROP FUNCTION IF EXISTS analyze_impact(UUID);

CREATE FUNCTION analyze_impact(p_file_id UUID)
RETURNS TABLE (
    root_file_id UUID,
    affected_file_id UUID,
    affected_file_name TEXT,
    impact_depth INTEGER,
    path TEXT,
    relationship_path TEXT,
    min_confidence NUMERIC(5,4),
    relationship_count INTEGER,
    path_count INTEGER,
    impact_score NUMERIC(8,6),
    impact_level TEXT
)
LANGUAGE sql
AS $function$

WITH RECURSIVE impact AS (

    -- Direct dependents of the changed file.
    SELECT
        p_file_id AS root_file_id,
        r.source_file_id AS affected_file_id,
        1 AS impact_depth,

        ARRAY[p_file_id, r.source_file_id]::UUID[] AS visited,

        ARRAY[root_file.name, affected_file.name]::TEXT[]
            AS file_path,

        ARRAY[r.relationship_type::TEXT] AS rel_path,

        r.confidence::NUMERIC(5,4) AS min_confidence,

        1 AS relationship_count,

        (
            r.confidence
            * rw.impact_weight
        )::NUMERIC(8,6) AS path_score

    FROM file_relationships r

    JOIN relationship_weights rw
        ON rw.relationship_type = r.relationship_type

    JOIN files root_file
        ON root_file.id = p_file_id

    JOIN files affected_file
        ON affected_file.id = r.source_file_id

    WHERE r.target_file_id = p_file_id


    UNION ALL


    -- Continue from the currently affected file to documents
    -- that depend on it.
    SELECT
        i.root_file_id,
        r.source_file_id AS affected_file_id,
        i.impact_depth + 1,

        i.visited || r.source_file_id,

        i.file_path || affected_file.name,

        i.rel_path || r.relationship_type::TEXT,

        LEAST(
            i.min_confidence,
            r.confidence
        )::NUMERIC(5,4),

        i.relationship_count + 1,

        (
            i.path_score
            * r.confidence
            * rw.impact_weight
            * 0.850000
        )::NUMERIC(8,6) AS path_score

    FROM impact i

    JOIN file_relationships r
        ON r.target_file_id = i.affected_file_id

    JOIN relationship_weights rw
        ON rw.relationship_type = r.relationship_type

    JOIN files affected_file
        ON affected_file.id = r.source_file_id

    WHERE NOT (
        r.source_file_id = ANY(i.visited)
    )
),

path_stats AS (
    SELECT
        i.affected_file_id,
        COUNT(*)::INTEGER AS path_count,
        MAX(i.path_score)::NUMERIC(8,6) AS impact_score
    FROM impact i
    GROUP BY i.affected_file_id
),

best_path AS (
    SELECT DISTINCT ON (i.affected_file_id)
        i.root_file_id,
        i.affected_file_id,
        i.impact_depth,
        i.file_path,
        i.rel_path,
        i.min_confidence,
        i.relationship_count,
        i.path_score
    FROM impact i
    ORDER BY
        i.affected_file_id,
        i.path_score DESC,
        i.min_confidence DESC,
        i.impact_depth ASC
)

SELECT
    b.root_file_id,
    b.affected_file_id,
    f.name AS affected_file_name,

    b.impact_depth,

    array_to_string(
        b.file_path,
        ' -> '
    ) AS path,

    array_to_string(
        b.rel_path,
        ' -> '
    ) AS relationship_path,

    b.min_confidence,

    b.relationship_count,

    s.path_count,

    s.impact_score,

    CASE
        WHEN s.impact_score >= 0.750000 THEN 'HIGH'
        WHEN s.impact_score >= 0.450000 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS impact_level

FROM best_path b

JOIN path_stats s
    ON s.affected_file_id = b.affected_file_id

JOIN files f
    ON f.id = b.affected_file_id

ORDER BY
    s.impact_score DESC,
    b.impact_depth ASC;

$function$;

COMMENT ON FUNCTION analyze_impact(UUID) IS
'Calculates ranked transitive document impact using relationship weights, edge confidence, and path distance.';